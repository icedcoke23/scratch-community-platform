package com.scratch.community.module.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.module.social.entity.ProjectLike;
import com.scratch.community.module.social.mapper.ProjectLikeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 个性化推荐服务
 *
 * 推荐策略（混合推荐）:
 * 1. 协同过滤: 基于用户点赞行为相似度推荐
 * 2. 热度推荐: 基于点赞数的热门项目
 * 3. 时间衰减: 新内容获得更高权重
 *
 * 冷启动策略:
 * - 新用户: 推荐热门项目 + 最新项目
 * - 活跃用户: 协同过滤 + 热度混合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CrossModuleQueryRepository crossModuleQueryRepository;
    private final ProjectLikeMapper projectLikeMapper;
    private final StringRedisTemplate redisTemplate;

    /** 推荐结果缓存时间（秒）: 10 分钟 */
    private static final long CACHE_TTL = 600;

    /** 每次推荐数量 */
    private static final int PAGE_SIZE = 20;

    /** 协同过滤权重 */
    private static final double W_COLLABORATIVE = 0.4;

    /** 热度权重 */
    private static final double W_POPULARITY = 0.3;

    /** 时间衰减权重 */
    private static final double W_RECENCY = 0.3;

    /** 时间衰减半衰期（天） */
    private static final double HALF_LIFE_DAYS = 7.0;

    /**
     * 获取个性化推荐 Feed
     *
     * @param userId 当前用户 ID（可为 null 表示未登录）
     * @param page   页码
     * @return 推荐的项目列表
     */
    public Page<Map<String, Object>> getRecommendations(Long userId, int page) {
        String cacheKey = "recommend:" + (userId != null ? userId : "anonymous") + ":" + page;

        Page<Map<String, Object>> resultPage = new Page<>(page, PAGE_SIZE);

        if (userId == null) {
            resultPage = getAnonymousRecommendations(resultPage);
        } else {
            resultPage = getPersonalizedRecommendations(userId, resultPage);
        }

        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, "1", CACHE_TTL, TimeUnit.SECONDS);

        return resultPage;
    }

    /**
     * 未登录用户的推荐策略: 热度 + 时间混合
     */
    private Page<Map<String, Object>> getAnonymousRecommendations(Page<Map<String, Object>> page) {
        int offset = (int) ((page.getCurrent() - 1) * PAGE_SIZE);
        List<Map<String, Object>> records = crossModuleQueryRepository.getPublishedProjectsByPopularity(PAGE_SIZE, offset);
        long total = crossModuleQueryRepository.getPublishedProjectCount();
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    /**
     * 已登录用户的个性化推荐
     */
    private Page<Map<String, Object>> getPersonalizedRecommendations(Long userId, Page<Map<String, Object>> page) {
        // 1. 获取用户点赞过的项目 ID
        Set<Long> likedProjectIds = getUserLikedProjectIds(userId);

        if (likedProjectIds.isEmpty()) {
            log.debug("用户 {} 冷启动，使用热门推荐", userId);
            return getAnonymousRecommendations(page);
        }

        // 2. 协同过滤: 找到相似用户喜欢的项目
        Set<Long> collaborativeCandidates = getCollaborativeCandidates(userId, likedProjectIds);

        // 3. 排除已点赞的
        List<Long> candidateIds = new ArrayList<>(collaborativeCandidates);
        candidateIds.removeAll(likedProjectIds);

        if (candidateIds.isEmpty()) {
            return getAnonymousRecommendations(page);
        }

        // 4. 批量查询候选项目
        List<Map<String, Object>> candidates = crossModuleQueryRepository.getProjectsByIds(candidateIds);

        // 5. 计算综合得分并排序
        List<ScoredProject> scored = candidates.stream()
                .filter(p -> "published".equals(p.get("status")))
                .map(p -> new ScoredProject(p, calculateScore(p, collaborativeCandidates.contains(((Number) p.get("id")).longValue()))))
                .sorted(Comparator.comparingDouble(ScoredProject::score).reversed())
                .toList();

        // 6. 分页截取
        int from = (int) (page.getCurrent() - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, scored.size());

        if (from >= scored.size()) {
            page.setRecords(Collections.emptyList());
            page.setTotal(scored.size());
            return page;
        }

        List<Map<String, Object>> result = scored.subList(from, to).stream()
                .map(ScoredProject::project)
                .toList();

        page.setRecords(result);
        page.setTotal(scored.size());
        return page;
    }

    /**
     * 计算项目综合得分
     */
    private double calculateScore(Map<String, Object> project, boolean isCollaborative) {
        double collabScore = isCollaborative ? 1.0 : 0.0;
        int likeCount = project.get("likeCount") != null ? ((Number) project.get("likeCount")).intValue() : 0;
        double popularityScore = Math.min(likeCount / 100.0, 1.0);

        LocalDateTime createdAt = project.get("createdAt") != null
                ? ((java.sql.Timestamp) project.get("createdAt")).toLocalDateTime()
                : LocalDateTime.now().minusDays(30);
        long daysSinceCreation = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
        double recencyScore = Math.exp(-0.693 * daysSinceCreation / HALF_LIFE_DAYS);

        return W_COLLABORATIVE * collabScore + W_POPULARITY * popularityScore + W_RECENCY * recencyScore;
    }

    /**
     * 协同过滤: 获取相似用户喜欢的项目
     */
    private Set<Long> getCollaborativeCandidates(Long userId, Set<Long> likedProjectIds) {
        List<ProjectLike> similarLikes = projectLikeMapper.selectList(
                new LambdaQueryWrapper<ProjectLike>()
                        .in(ProjectLike::getProjectId, likedProjectIds)
                        .ne(ProjectLike::getUserId, userId)
                        .last("LIMIT 500"));

        Map<Long, Long> projectFrequency = similarLikes.stream()
                .collect(Collectors.groupingBy(ProjectLike::getProjectId, Collectors.counting()));

        return projectFrequency.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(100)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * 获取用户点赞过的项目 ID
     */
    private Set<Long> getUserLikedProjectIds(Long userId) {
        List<ProjectLike> likes = projectLikeMapper.selectList(
                new LambdaQueryWrapper<ProjectLike>()
                        .eq(ProjectLike::getUserId, userId)
                        .select(ProjectLike::getProjectId));
        return likes.stream()
                .map(ProjectLike::getProjectId)
                .collect(Collectors.toSet());
    }

    /**
     * 清除用户推荐缓存（点赞/取消点赞时调用）
     */
    public void invalidateCache(Long userId) {
        String pattern = "recommend:" + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private record ScoredProject(Map<String, Object> project, double score) {}
}
