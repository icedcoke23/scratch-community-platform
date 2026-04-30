package com.scratch.community.module.social.service;

import com.scratch.community.module.social.vo.RankVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排行榜服务（基于 Redis Sorted Set）
 *
 * 排行榜维度：
 * - like_rank:weekly  — 周点赞排行榜
 * - like_rank:monthly — 月点赞排行榜
 * - project_rank:weekly — 周作品排行榜（按作品数）
 *
 * 改进:
 * - getRank() 批量查询用户信息（消除 N+1）
 * - refreshRankings() 先构建新数据再原子替换（消除数据真空）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankService {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    private static final String LIKE_RANK_WEEKLY = "rank:like:weekly";
    private static final String LIKE_RANK_MONTHLY = "rank:like:monthly";
    private static final String PROJECT_RANK_WEEKLY = "rank:project:weekly";

    /** 临时 key 后缀，用于原子替换 */
    private static final String TEMP_SUFFIX = ":temp";

    /**
     * 初始化排行榜（启动时加载）
     */
    @PostConstruct
    public void init() {
        refreshRankings();
    }

    /**
     * 每小时刷新排行榜
     */
    @Scheduled(fixedRate = 3600000)
    public void refreshRankings() {
        try {
            refreshLikeRank(LIKE_RANK_WEEKLY, "WEEK");
            refreshLikeRank(LIKE_RANK_MONTHLY, "MONTH");
            refreshProjectRank(PROJECT_RANK_WEEKLY, "WEEK");
            log.info("排行榜刷新完成");
        } catch (Exception e) {
            log.error("排行榜刷新失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 增加用户点赞数（实时更新排行榜分数）
     */
    public void incrementLikeScore(Long userId, int delta) {
        redisTemplate.opsForZSet().incrementScore(LIKE_RANK_WEEKLY, userId.toString(), delta);
        redisTemplate.opsForZSet().incrementScore(LIKE_RANK_MONTHLY, userId.toString(), delta);
    }

    /**
     * 获取周点赞排行榜
     */
    public List<RankVO> getWeeklyLikeRank(int topN) {
        return getRank(LIKE_RANK_WEEKLY, topN);
    }

    /**
     * 获取月点赞排行榜
     */
    public List<RankVO> getMonthlyLikeRank(int topN) {
        return getRank(LIKE_RANK_MONTHLY, topN);
    }

    /**
     * 获取周作品排行榜
     */
    public List<RankVO> getWeeklyProjectRank(int topN) {
        return getRank(PROJECT_RANK_WEEKLY, topN);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取排行榜（批量查询用户信息，消除 N+1）
     */
    private List<RankVO> getRank(String key, int topN) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);

        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        // 收集所有 userId
        List<Long> userIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userIdStr = tuple.getValue();
            Double score = tuple.getScore();
            if (userIdStr == null || score == null) continue;
            Long userId = Long.parseLong(userIdStr);
            userIds.add(userId);
            scoreMap.put(userId, score);
        }

        if (userIds.isEmpty()) {
            return List.of();
        }

        // 批量查询用户信息（消除 N+1）
        String inClause = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        Map<Long, RankVO> userMap = new HashMap<>();
        try {
            jdbcTemplate.query(
                    "SELECT id, username, nickname, avatar_url FROM user WHERE id IN (" + inClause + ") AND deleted = 0",
                    (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                        Long uid = rs.getLong("id");
                        RankVO vo = new RankVO();
                        vo.setUserId(uid);
                        vo.setUsername(rs.getString("username"));
                        vo.setNickname(rs.getString("nickname"));
                        vo.setAvatarUrl(rs.getString("avatar_url"));
                        userMap.put(uid, vo);
                    },
                    userIds.toArray());
        } catch (Exception e) {
            log.warn("批量查询排行榜用户信息失败: {}", e.getMessage());
        }

        // 组装结果（保持 Redis 排序顺序）
        List<RankVO> result = new ArrayList<>();
        long rank = 1;
        for (Long userId : userIds) {
            RankVO vo = userMap.get(userId);
            if (vo == null) {
                vo = new RankVO();
                vo.setUserId(userId);
            }
            vo.setRank(rank++);
            vo.setLikeCount(scoreMap.getOrDefault(userId, 0.0).intValue());
            result.add(vo);
        }

        return result;
    }

    /**
     * 刷新点赞排行榜（先构建再原子替换，消除数据真空）
     */
    private void refreshLikeRank(String key, String interval) {
        String tempKey = key + TEMP_SUFFIX;

        // 1. 先清空临时 key
        redisTemplate.delete(tempKey);

        // 2. 构建新数据到临时 key
        String sql = "SELECT u.id, COUNT(pl.id) AS like_count " +
                "FROM user u LEFT JOIN project_like pl ON u.id = pl.user_id " +
                (interval.equals("WEEK") ? "AND pl.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " :
                        "AND pl.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) ") +
                "WHERE u.deleted = 0 " +
                "GROUP BY u.id HAVING like_count > 0 ORDER BY like_count DESC LIMIT 100";

        jdbcTemplate.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            Long userId = rs.getLong("id");
            int likeCount = rs.getInt("like_count");
            redisTemplate.opsForZSet().add(tempKey, userId.toString(), likeCount);
        });

        // 3. 原子替换：RENAME tempKey -> key（仅当 tempKey 有数据时）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(tempKey))) {
            redisTemplate.rename(tempKey, key);
            log.debug("排行榜已刷新: key={}, interval={}", key, interval);
        } else {
            log.debug("排行榜无数据，跳过替换: key={}, interval={}", key, interval);
        }
    }

    /**
     * 刷新作品排行榜（先构建再原子替换）
     */
    private void refreshProjectRank(String key, String interval) {
        String tempKey = key + TEMP_SUFFIX;
        redisTemplate.delete(tempKey);

        String sql = "SELECT u.id, COUNT(p.id) AS project_count " +
                "FROM user u LEFT JOIN project p ON u.id = p.user_id AND p.status = 'published' " +
                (interval.equals("WEEK") ? "AND p.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " :
                        "AND p.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) ") +
                "WHERE u.deleted = 0 " +
                "GROUP BY u.id HAVING project_count > 0 ORDER BY project_count DESC LIMIT 100";

        jdbcTemplate.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            Long userId = rs.getLong("id");
            int projectCount = rs.getInt("project_count");
            redisTemplate.opsForZSet().add(tempKey, userId.toString(), projectCount);
        });

        if (Boolean.TRUE.equals(redisTemplate.hasKey(tempKey))) {
            redisTemplate.rename(tempKey, key);
            log.debug("作品排行榜已刷新: key={}, interval={}", key, interval);
        } else {
            log.debug("作品排行榜无数据，跳过替换: key={}, interval={}", key, interval);
        }
    }
}
