package com.scratch.community.common.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * 项目模块跨模块查询仓库（只读）
 *
 * <p>从 {@link CrossModuleQueryRepository} 拆分，专注于项目相关的跨模块查询。
 * 各模块通过此 Repository 访问项目表数据，而非直接注入 JdbcTemplate。
 *
 * @see CrossModuleQueryRepository 通用跨模块查询（保留兼容）
 * @see CrossModuleWriteRepository 跨模块写操作
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProjectQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 检查项目是否存在且未删除
     */
    public boolean projectExists(Long projectId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE id = ? AND deleted = 0",
                Integer.class, projectId);
        return count != null && count > 0;
    }

    /**
     * 获取项目作者 ID
     */
    public Long getProjectOwnerId(Long projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT user_id FROM project WHERE id = ? AND deleted = 0",
                Long.class, projectId);
    }

    /**
     * 获取项目基本信息
     */
    public Map<String, Object> getProjectInfo(Long projectId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, parse_result, block_count, complexity_score, status " +
                "FROM project WHERE id = ? AND deleted = 0",
                (org.springframework.jdbc.core.ResultSetExtractor<Map<String, Object>>) rs -> {
                    if (!rs.next()) return null;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("user_id", rs.getLong("user_id"));
                    row.put("parse_result", rs.getString("parse_result"));
                    row.put("block_count", rs.getInt("block_count"));
                    row.put("complexity_score", rs.getDouble("complexity_score"));
                    row.put("status", rs.getString("status"));
                    return row;
                },
                projectId);
    }

    /**
     * 获取项目列表（Feed 流）— 支持排序
     */
    public List<Map<String, Object>> getProjectFeed(String sort, int offset, int limit) {
        String orderBy = "hot".equals(sort)
                ? "p.like_count DESC, p.created_at DESC"
                : "p.created_at DESC";
        String sql = "SELECT p.id, p.title, p.cover_url, p.like_count, p.comment_count, p.view_count, " +
                "p.block_count, p.complexity_score, p.tags, p.created_at, p.status, " +
                "u.id AS author_id, u.nickname AS author_nickname, u.avatar_url AS author_avatar " +
                "FROM project p JOIN user u ON p.user_id = u.id " +
                "WHERE p.status = 'published' AND p.deleted = 0 AND u.deleted = 0 " +
                "ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("title", rs.getString("title"));
            row.put("coverUrl", rs.getString("cover_url"));
            row.put("likeCount", rs.getInt("like_count"));
            row.put("commentCount", rs.getInt("comment_count"));
            row.put("viewCount", rs.getInt("view_count"));
            row.put("blockCount", rs.getInt("block_count"));
            row.put("complexityScore", rs.getDouble("complexity_score"));
            row.put("tags", rs.getString("tags"));
            row.put("createdAt", rs.getTimestamp("created_at"));
            row.put("status", rs.getString("status"));
            row.put("authorId", rs.getLong("author_id"));
            row.put("authorNickname", rs.getString("author_nickname"));
            row.put("authorAvatar", rs.getString("author_avatar"));
            return row;
        }, limit, offset);
    }

    /**
     * 获取已发布项目总数
     */
    public long getPublishedProjectCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE status = 'published' AND deleted = 0",
                Long.class);
        return count != null ? count : 0;
    }

    /**
     * 统计项目总数
     */
    public long countAllProjects() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE deleted = 0", Long.class);
        return count != null ? count : 0;
    }

    /**
     * 按状态统计项目数
     */
    public long countProjectsByStatus(String status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE status = ? AND deleted = 0",
                Long.class, status);
        return count != null ? count : 0;
    }

    /**
     * 统计指定时间段内创建的项目数
     */
    public long countProjectsCreatedBetween(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE deleted = 0 AND created_at >= ? AND created_at <= ?",
                Long.class, start, end);
        return count != null ? count : 0;
    }

    /**
     * 获取已发布的项目（按热度+时间排序，分页）
     */
    public List<Map<String, Object>> getPublishedProjectsByPopularity(int limit, int offset) {
        return jdbcTemplate.query(
                "SELECT id, user_id, title, cover_url, status, like_count, comment_count, view_count, " +
                "block_count, complexity_score, tags, created_at " +
                "FROM project WHERE status = 'published' AND deleted = 0 " +
                "ORDER BY like_count DESC, created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("userId", rs.getLong("user_id"));
                    row.put("title", rs.getString("title"));
                    row.put("coverUrl", rs.getString("cover_url"));
                    row.put("status", rs.getString("status"));
                    row.put("likeCount", rs.getInt("like_count"));
                    row.put("commentCount", rs.getInt("comment_count"));
                    row.put("viewCount", rs.getInt("view_count"));
                    row.put("blockCount", rs.getInt("block_count"));
                    row.put("complexityScore", rs.getDouble("complexity_score"));
                    row.put("tags", rs.getString("tags"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    return row;
                },
                limit, offset);
    }

    /**
     * 根据 ID 列表批量获取项目
     */
    public List<Map<String, Object>> getProjectsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        String inClause = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
        String sql = "SELECT id, user_id, title, cover_url, status, like_count, comment_count, view_count, " +
                "block_count, complexity_score, tags, created_at " +
                "FROM project WHERE id IN (" + inClause + ") AND deleted = 0";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("userId", rs.getLong("user_id"));
            row.put("title", rs.getString("title"));
            row.put("coverUrl", rs.getString("cover_url"));
            row.put("status", rs.getString("status"));
            row.put("likeCount", rs.getInt("like_count"));
            row.put("commentCount", rs.getInt("comment_count"));
            row.put("viewCount", rs.getInt("view_count"));
            row.put("blockCount", rs.getInt("block_count"));
            row.put("complexityScore", rs.getDouble("complexity_score"));
            row.put("tags", rs.getString("tags"));
            row.put("createdAt", rs.getTimestamp("created_at"));
            return row;
        }, ids.toArray());
    }

    /**
     * 批量查询用户是否已点赞指定项目
     */
    public Set<Long> getLikedProjectIds(Long userId, List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptySet();
        String inClause = projectIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
        String sql = "SELECT project_id FROM project_like WHERE user_id = ? AND project_id IN (" + inClause + ")";
        Object[] params = new Object[projectIds.size() + 1];
        params[0] = userId;
        for (int i = 0; i < projectIds.size(); i++) params[i + 1] = projectIds.get(i);
        Set<Long> liked = new HashSet<>();
        jdbcTemplate.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                liked.add(rs.getLong("project_id")), params);
        return liked;
    }
}
