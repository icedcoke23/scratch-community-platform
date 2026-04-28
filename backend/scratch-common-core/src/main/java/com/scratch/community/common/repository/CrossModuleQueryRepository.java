package com.scratch.community.common.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 跨模块查询仓库（只读）
 *
 * 将散落在各 Service 中的跨模块 JdbcTemplate 查询集中管理，
 * 避免 SQL 散落在业务代码中难以维护和审计。
 *
 * <p>设计原则:
 * <ul>
 *   <li>所有跨模块的裸 SQL <b>查询</b> 都应放在这里</li>
 *   <li>本类只提供<b>只读查询</b>方法，写操作请使用 {@link CrossModuleWriteRepository}</li>
 *   <li>所有查询使用参数化防止 SQL 注入</li>
 *   <li>方法命名清晰表达查询意图</li>
 * </ul>
 *
 * <p>模块边界:
 * 各模块通过此 Repository 访问其他模块的表数据，而非直接注入 JdbcTemplate。
 * 未来如果需要模块间解耦（如微服务拆分），只需替换此 Repository 的实现。
 *
 * <p><b>重构说明</b>: 本类中的方法已按模块拆分为独立的 Repository：
 * <ul>
 *   <li>{@link ProjectQueryRepository} — 项目相关查询</li>
 *   <li>{@link UserQueryRepository} — 用户相关查询</li>
 * </ul>
 * 新代码请使用对应的模块化 Repository，本类保留用于向后兼容。
 *
 * @see CrossModuleWriteRepository 跨模块写操作仓库
 * @see ProjectQueryRepository 项目模块查询
 * @see UserQueryRepository 用户模块查询
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CrossModuleQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    // ==================== 项目相关 ====================

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
     * 获取项目基本信息（id, user_id, parse_result, block_count, complexity_score, status）
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

    // ==================== 用户相关 ====================

    /**
     * 获取用户基本信息（username, nickname, avatar_url）
     *
     * <p>使用 Caffeine 本地缓存，10 分钟过期，最大 1000 条。
     * 适用于高频跨模块查询（如项目详情页需要展示作者信息）。
     */
    @Cacheable(cacheNames = "userInfo", key = "'basic:' + #userId")
    public Map<String, Object> getUserBasicInfo(Long userId) {
        return jdbcTemplate.query(
                "SELECT username, nickname, avatar_url FROM user WHERE id = ? AND deleted = 0",
                (org.springframework.jdbc.core.ResultSetExtractor<Map<String, Object>>) rs -> {
                    if (!rs.next()) return null;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("username", rs.getString("username"));
                    row.put("nickname", rs.getString("nickname"));
                    row.put("avatar_url", rs.getString("avatar_url"));
                    return row;
                },
                userId);
    }

    /**
     * 获取用户积分
     */
    public int getUserPoints(Long userId) {
        Integer points = jdbcTemplate.queryForObject(
                "SELECT COALESCE(points, 0) FROM user WHERE id = ? AND deleted = 0",
                Integer.class, userId);
        return points != null ? points : 0;
    }

    // ==================== 作业相关 ====================

    /**
     * 获取班级的作业统计（含平均分，单次聚合查询替代 N+1）
     */
    public List<Map<String, Object>> getHomeworkStats(Long classId) {
        return jdbcTemplate.query(
                "SELECT h.id, h.title, h.submit_count, h.graded_count, h.total_score, h.status, h.deadline, " +
                "COALESCE(AVG(hs.score), 0) AS avg_score " +
                "FROM homework h " +
                "LEFT JOIN homework_submission hs ON h.id = hs.homework_id AND hs.status = 'graded' AND hs.deleted = 0 " +
                "WHERE h.class_id = ? AND h.deleted = 0 " +
                "GROUP BY h.id, h.title, h.submit_count, h.graded_count, h.total_score, h.status, h.deadline " +
                "ORDER BY h.created_at DESC",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("title", rs.getString("title"));
                    row.put("submitCount", rs.getInt("submit_count"));
                    row.put("gradedCount", rs.getInt("graded_count"));
                    row.put("totalScore", rs.getInt("total_score"));
                    row.put("status", rs.getString("status"));
                    row.put("deadline", rs.getTimestamp("deadline"));
                    row.put("avgScore", rs.getDouble("avg_score"));
                    return row;
                },
                classId);
    }

    // ==================== 积分相关 ====================

    /**
     * 获取用户今日指定类型的积分总和
     */
    public int getTodayPointsByType(Long userId, String type) {
        Integer points = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(points), 0) FROM point_log " +
                "WHERE user_id = ? AND type = ? AND DATE(created_at) = CURDATE()",
                Integer.class, userId, type);
        return points != null ? points : 0;
    }

    /**
     * 检查用户今日是否已签到
     */
    public boolean hasCheckedInToday(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM point_log WHERE user_id = ? AND type = 'DAILY_CHECKIN' AND DATE(created_at) = CURDATE()",
                Integer.class, userId);
        return count != null && count > 0;
    }

    // ==================== 评论查询 ====================

    /**
     * 获取评论详情
     */
    public Map<String, Object> getCommentById(Long commentId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, project_id, content, created_at FROM project_comment WHERE id = ? AND deleted = 0",
                (org.springframework.jdbc.core.ResultSetExtractor<Map<String, Object>>) rs -> {
                    if (!rs.next()) return null;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("userId", rs.getLong("user_id"));
                    row.put("projectId", rs.getLong("project_id"));
                    row.put("content", rs.getString("content"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    return row;
                },
                commentId);
    }

    // ==================== 社区 Feed 查询 ====================

    /**
     * 获取项目列表（Feed 流）— 支持排序
     *
     * @deprecated 此方法与 {@code FeedService.getFeed()} 存在重复逻辑。
     * FeedService 的实现更完善（支持 FULLTEXT 搜索、多排序选项、点赞状态查询）。
     * 新代码请直接使用 FeedService，本方法保留用于向后兼容。
     */
    @Deprecated
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
     *
     * @deprecated 与 FeedService 中的 countSql 存在重复，新代码请使用 FeedService。
     */
    @Deprecated
    public long getPublishedProjectCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE status = 'published' AND deleted = 0",
                Long.class);
        return count != null ? count : 0;
    }

    /**
     * 批量查询用户是否已点赞指定项目
     *
     * @deprecated 与 FeedService 中的点赞查询逻辑重复，新代码请使用 FeedService。
     */
    @Deprecated
    public Set<Long> getLikedProjectIds(Long userId, List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptySet();
        String inClause = projectIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT project_id FROM project_like WHERE user_id = ? AND project_id IN (" + inClause + ")";
        Object[] params = new Object[projectIds.size() + 1];
        params[0] = userId;
        for (int i = 0; i < projectIds.size(); i++) params[i + 1] = projectIds.get(i);
        Set<Long> liked = new HashSet<>();
        jdbcTemplate.query(sql, (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                liked.add(rs.getLong("project_id")), params);
        return liked;
    }

    // ==================== 用户统计 ====================

    /**
     * 统计学生总数
     */
    public long countStudents() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE role = 'STUDENT' AND deleted = 0",
                Long.class);
        return count != null ? count : 0;
    }

    /**
     * 统计指定时间后活跃的学生数
     */
    public long countStudentsActiveSince(java.time.LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE role = 'STUDENT' AND deleted = 0 AND updated_at >= ?",
                Long.class, since);
        return count != null ? count : 0;
    }

    /**
     * 统计指定时间段内活跃的学生数
     */
    public long countStudentsActiveBetween(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE role = 'STUDENT' AND deleted = 0 AND updated_at >= ? AND updated_at <= ?",
                Long.class, start, end);
        return count != null ? count : 0;
    }

    /**
     * 获取学生等级和积分信息
     */
    public List<Map<String, Object>> getStudentLevelAndPoints() {
        return jdbcTemplate.query(
                "SELECT id, nickname, level, points FROM user WHERE role = 'STUDENT' AND deleted = 0",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("nickname", rs.getString("nickname"));
                    row.put("level", rs.getInt("level"));
                    row.put("points", rs.getInt("points"));
                    return row;
                });
    }

    /**
     * 获取不活跃学生列表（指定时间前最后活跃）
     */
    public List<Map<String, Object>> getInactiveStudentsSince(java.time.LocalDateTime since, int limit) {
        return jdbcTemplate.query(
                "SELECT id, nickname, updated_at FROM user " +
                "WHERE role = 'STUDENT' AND deleted = 0 AND updated_at < ? LIMIT ?",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("nickname", rs.getString("nickname"));
                    row.put("updatedAt", rs.getTimestamp("updated_at"));
                    return row;
                },
                since, limit);
    }

    // ==================== 作业统计 ====================

    /**
     * 统计班级作业数
     */
    public long countHomeworkByClass(Long classId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework WHERE class_id = ? AND deleted = 0",
                Long.class, classId);
        return count != null ? count : 0;
    }

    /**
     * 获取班级作业列表（按创建时间倒序）
     */
    public List<Map<String, Object>> getHomeworkListByClass(Long classId, int limit) {
        return jdbcTemplate.query(
                "SELECT id, title, deadline, created_at FROM homework " +
                "WHERE class_id = ? AND deleted = 0 ORDER BY created_at DESC LIMIT ?",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("title", rs.getString("title"));
                    row.put("deadline", rs.getTimestamp("deadline"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    return row;
                },
                classId, limit);
    }

    /**
     * 统计作业提交数
     */
    public long countSubmissionsByHomework(Long homeworkId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM homework_submission WHERE homework_id = ? AND deleted = 0",
                Long.class, homeworkId);
        return count != null ? count : 0;
    }

    // ==================== 项目统计 ====================

    /**
     * 统计项目总数
     */
    public long countAllProjects() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE deleted = 0",
                Long.class);
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
        String inClause = ids.stream().map(id -> "?").collect(Collectors.joining(","));
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

    // ==================== 积分排行榜 ====================

    /**
     * 获取积分排行榜（JOIN 替代关联子查询，单次聚合）
     */
    public List<Map<String, Object>> getPointRanking(int topN) {
        return jdbcTemplate.query(
                "SELECT u.id, u.username, u.nickname, u.avatar_url, " +
                "COALESCE(SUM(pl.points), 0) AS total_points " +
                "FROM user u " +
                "LEFT JOIN point_log pl ON u.id = pl.user_id " +
                "WHERE u.deleted = 0 AND u.status = 1 " +
                "GROUP BY u.id, u.username, u.nickname, u.avatar_url " +
                "ORDER BY total_points DESC LIMIT ?",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("username", rs.getString("username"));
                    row.put("nickname", rs.getString("nickname"));
                    row.put("avatarUrl", rs.getString("avatar_url"));
                    row.put("points", rs.getInt("total_points"));
                    return row;
                },
                topN);
    }
}
