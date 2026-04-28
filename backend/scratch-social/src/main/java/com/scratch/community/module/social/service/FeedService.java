package com.scratch.community.module.social.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.module.social.mapper.ProjectLikeMapper;
import com.scratch.community.module.social.vo.FeedVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Feed 流服务（最新 + 最热排序）
 *
 * 通过 JdbcTemplate 直接查询数据库实现跨模块关联。
 * 所有 SQL 使用参数化查询，防止 SQL 注入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final JdbcTemplate jdbcTemplate;
    private final ProjectLikeMapper projectLikeMapper;

    /**
     * 获取社区项目列表（分页）
     *
     * @param sort 排序方式: latest(最新) / hot(最热)
     * @param currentUserId 当前用户 ID（用于判断是否已点赞），null 表示未登录
     */
    public Page<FeedVO> getFeed(String sort, Long currentUserId, Page<FeedVO> page) {
        long offset = (page.getCurrent() - 1) * page.getSize();
        if (offset < 0) offset = 0;

        // 白名单校验排序参数，防 SQL 注入
        // 热度公式：互动分 / (时间衰减因子 + 2)^1.5
        // 参考 Hacker News 算法，新内容获得初始曝光，优质内容持续排名靠前
        String orderBy;
        if ("hot".equals(sort)) {
            orderBy = "(p.like_count + p.comment_count * 2 + p.view_count * 0.1) " +
                    "/ POW(GREATEST(TIMESTAMPDIFF(HOUR, p.created_at, NOW()), 0) + 2, 1.5) DESC";
        } else {
            orderBy = "p.created_at DESC";
        }

        // 查询总数
        String countSql = "SELECT COUNT(*) FROM project p WHERE p.status = 'published' AND p.deleted = 0";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);

        // 查询列表（参数化 LIMIT/OFFSET）
        String querySql = "SELECT p.id, p.user_id, p.title, p.description, p.cover_url, p.tags, " +
                "p.block_count, p.complexity_score, p.like_count, p.comment_count, p.view_count, " +
                "p.created_at, u.username, u.nickname, u.avatar_url " +
                "FROM project p JOIN user u ON p.user_id = u.id AND u.deleted = 0 " +
                "WHERE p.status = 'published' AND p.deleted = 0 " +
                "ORDER BY " + orderBy + " LIMIT ? OFFSET ?";

        List<FeedVO> records = jdbcTemplate.query(querySql, (rs, rowNum) -> {
            FeedVO vo = new FeedVO();
            vo.setId(rs.getLong("id"));
            vo.setUserId(rs.getLong("user_id"));
            vo.setTitle(rs.getString("title"));
            vo.setDescription(rs.getString("description"));
            vo.setCoverUrl(rs.getString("cover_url"));
            vo.setTags(rs.getString("tags"));
            vo.setBlockCount(rs.getInt("block_count"));
            vo.setComplexityScore(rs.getDouble("complexity_score"));
            vo.setLikeCount(rs.getInt("like_count"));
            vo.setCommentCount(rs.getInt("comment_count"));
            vo.setViewCount(rs.getInt("view_count"));
            vo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            vo.setUsername(rs.getString("username"));
            vo.setNickname(rs.getString("nickname"));
            vo.setAvatarUrl(rs.getString("avatar_url"));
            vo.setIsLiked(false);
            return vo;
        }, page.getSize(), offset);

        // 批量查询当前用户的点赞状态（参数化 IN 查询，防止 SQL 注入）
        if (currentUserId != null && !records.isEmpty()) {
            List<Long> projectIds = records.stream().map(FeedVO::getId).collect(Collectors.toList());

            // 构建参数化 IN 子句: "?,?,?"
            String inClause = projectIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String likeSql = "SELECT project_id FROM project_like WHERE user_id = ? AND project_id IN (" + inClause + ")";

            // 构建参数数组: [userId, projectId1, projectId2, ...]
            Object[] params = new Object[projectIds.size() + 1];
            params[0] = currentUserId;
            for (int i = 0; i < projectIds.size(); i++) {
                params[i + 1] = projectIds.get(i);
            }

            Set<Long> likedProjectIds = new HashSet<>();
            try {
                // 显式使用 RowCallbackHandler 避免 query() 重载歧义
                jdbcTemplate.query(likeSql,
                        (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                                likedProjectIds.add(rs.getLong("project_id")),
                        params);
            } catch (Exception e) {
                log.warn("批量查询点赞状态失败: {}", e.getMessage());
            }
            records.forEach(vo -> vo.setIsLiked(likedProjectIds.contains(vo.getId())));
        }

        Page<FeedVO> result = new Page<>(page.getCurrent(), page.getSize(), total != null ? total : 0);
        result.setRecords(records);
        return result;
    }

    /**
     * 全文搜索项目（使用 MySQL FULLTEXT 索引 ft_search）
     *
     * @param keyword       搜索关键词
     * @param currentUserId 当前用户 ID（用于判断点赞状态），null 表示未登录
     * @param page          分页参数
     * @return 搜索结果
     */
    public Page<FeedVO> search(String keyword, Long currentUserId, Page<FeedVO> page) {
        long offset = (page.getCurrent() - 1) * page.getSize();
        if (offset < 0) offset = 0;

        // 清理 FULLTEXT BOOLEAN MODE 特殊字符，防止语法错误
        String safeKeyword = sanitizeFulltextKeyword(keyword);

        // 参数化全文搜索，防 SQL 注入
        String countSql = "SELECT COUNT(*) FROM project p " +
                "WHERE p.status = 'published' AND p.deleted = 0 " +
                "AND MATCH(p.title, p.description) AGAINST(? IN BOOLEAN MODE)";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, safeKeyword);

        String querySql = "SELECT p.id, p.user_id, p.title, p.description, p.cover_url, p.tags, " +
                "p.block_count, p.complexity_score, p.like_count, p.comment_count, p.view_count, " +
                "p.created_at, u.username, u.nickname, u.avatar_url, " +
                "MATCH(p.title, p.description) AGAINST(? IN BOOLEAN MODE) AS relevance " +
                "FROM project p JOIN user u ON p.user_id = u.id AND u.deleted = 0 " +
                "WHERE p.status = 'published' AND p.deleted = 0 " +
                "AND MATCH(p.title, p.description) AGAINST(? IN BOOLEAN MODE) " +
                "ORDER BY relevance DESC, p.created_at DESC LIMIT ? OFFSET ?";

        List<FeedVO> records = jdbcTemplate.query(querySql, (rs, rowNum) -> {
            FeedVO vo = new FeedVO();
            vo.setId(rs.getLong("id"));
            vo.setUserId(rs.getLong("user_id"));
            vo.setTitle(rs.getString("title"));
            vo.setDescription(rs.getString("description"));
            vo.setCoverUrl(rs.getString("cover_url"));
            vo.setTags(rs.getString("tags"));
            vo.setBlockCount(rs.getInt("block_count"));
            vo.setComplexityScore(rs.getDouble("complexity_score"));
            vo.setLikeCount(rs.getInt("like_count"));
            vo.setCommentCount(rs.getInt("comment_count"));
            vo.setViewCount(rs.getInt("view_count"));
            vo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            vo.setUsername(rs.getString("username"));
            vo.setNickname(rs.getString("nickname"));
            vo.setAvatarUrl(rs.getString("avatar_url"));
            vo.setIsLiked(false);
            return vo;
        }, safeKeyword, safeKeyword, page.getSize(), offset);

        // 批量查询点赞状态
        if (currentUserId != null && !records.isEmpty()) {
            List<Long> projectIds = records.stream().map(FeedVO::getId).collect(Collectors.toList());
            String inClause = projectIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String likeSql = "SELECT project_id FROM project_like WHERE user_id = ? AND project_id IN (" + inClause + ")";

            Object[] params = new Object[projectIds.size() + 1];
            params[0] = currentUserId;
            for (int i = 0; i < projectIds.size(); i++) {
                params[i + 1] = projectIds.get(i);
            }

            Set<Long> likedProjectIds = new HashSet<>();
            try {
                jdbcTemplate.query(likeSql,
                        (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                                likedProjectIds.add(rs.getLong("project_id")),
                        params);
            } catch (Exception e) {
                log.warn("批量查询点赞状态失败: {}", e.getMessage());
            }
            records.forEach(vo -> vo.setIsLiked(likedProjectIds.contains(vo.getId())));
        }

        Page<FeedVO> result = new Page<>(page.getCurrent(), page.getSize(), total != null ? total : 0);
        result.setRecords(records);
        return result;
    }

    /**
     * 清理 FULLTEXT BOOLEAN MODE 特殊字符
     * <p>防止用户输入的 + - * > < ~ 等字符导致语法错误
     */
    private String sanitizeFulltextKeyword(String keyword) {
        if (keyword == null) return "";
        // 移除 BOOLEAN MODE 操作符，保留中英文和数字
        return keyword.replaceAll("[+\\-><~*()\"']", " ").trim();
    }
}
