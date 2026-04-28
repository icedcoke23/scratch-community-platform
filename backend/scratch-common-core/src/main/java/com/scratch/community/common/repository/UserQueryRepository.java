package com.scratch.community.common.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * 用户模块跨模块查询仓库（只读）
 *
 * <p>从 {@link CrossModuleQueryRepository} 拆分，专注于用户相关的跨模块查询。
 *
 * @see CrossModuleQueryRepository 通用跨模块查询（保留兼容）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JdbcTemplate jdbcTemplate;

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

    /**
     * 获取积分排行榜（直接使用 user.points 冗余字段，避免 JOIN point_log）
     */
    public List<Map<String, Object>> getPointRanking(int topN) {
        return jdbcTemplate.query(
                "SELECT id, username, nickname, avatar_url, points " +
                "FROM user WHERE deleted = 0 AND status = 1 " +
                "ORDER BY points DESC LIMIT ?",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("username", rs.getString("username"));
                    row.put("nickname", rs.getString("nickname"));
                    row.put("avatarUrl", rs.getString("avatar_url"));
                    row.put("points", rs.getInt("points"));
                    return row;
                },
                topN);
    }
}
