package com.scratch.community.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 冗余计数字段校准调度器
 *
 * <p>每天凌晨 3 点自动校准以下冗余计数字段，确保数据一致性：
 * <ul>
 *   <li>{@code project.like_count} — 与 {@code project_like} 表实际行数对齐</li>
 *   <li>{@code project.comment_count} — 与 {@code project_comment} 表实际行数对齐</li>
 *   <li>{@code project.view_count} — 保留不动（浏览数不可逆向推算）</li>
 *   <li>{@code user.points} — 与 {@code point_log} 表实际积分总和对齐</li>
 * </ul>
 *
 * <p>使用单条 UPDATE + 子查询，避免逐行循环，对大表友好。
 *
 * @author scratch-community
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountCalibrationScheduler {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 每天凌晨 3 点执行校准
     */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "calibrateCounts", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    public void calibrateCounts() {
        log.info("开始冗余计数字段校准...");
        long start = System.currentTimeMillis();

        int likeRows = calibrateLikeCount();
        int commentRows = calibrateCommentCount();
        int pointRows = calibrateUserPoints();

        long elapsed = System.currentTimeMillis() - start;
        log.info("冗余计数字段校准完成: like_count 校准 {} 行, comment_count 校准 {} 行, " +
                        "user.points 校准 {} 行, 耗时 {}ms",
                likeRows, commentRows, pointRows, elapsed);
    }

    /**
     * 每天凌晨 4 点清理过期通知
     *
     * <p>策略：
     * <ul>
     *   <li>已读通知保留 90 天后自动删除</li>
     *   <li>未读通知保留 180 天后自动删除（避免用户长期不登录导致通知堆积）</li>
     * </ul>
     *
     * <p>使用 DELETE LIMIT 分批删除，避免长事务锁表。
     */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "cleanupNotifications", lockAtLeastFor = "1m", lockAtMostFor = "30m")
    public void cleanupNotifications() {
        log.info("开始清理过期通知...");
        long start = System.currentTimeMillis();

        // 清理 90 天前的已读通知
        int readDeleted = jdbcTemplate.update(
                "DELETE FROM notification WHERE is_read = 1 AND created_at < DATE_SUB(NOW(), INTERVAL 90 DAY) LIMIT 10000");

        // 清理 180 天前的未读通知
        int unreadDeleted = jdbcTemplate.update(
                "DELETE FROM notification WHERE is_read = 0 AND created_at < DATE_SUB(NOW(), INTERVAL 180 DAY) LIMIT 10000");

        long elapsed = System.currentTimeMillis() - start;
        log.info("过期通知清理完成: 已读删除 {} 条, 未读删除 {} 条, 耗时 {}ms",
                readDeleted, unreadDeleted, elapsed);
    }

    /**
     * 校准 project.like_count
     *
     * <p>只更新实际值与计数值不一致的行，减少写放大。
     */
    private int calibrateLikeCount() {
        return jdbcTemplate.update(
                "UPDATE project p " +
                "SET like_count = (" +
                "    SELECT COUNT(*) FROM project_like pl " +
                "    WHERE pl.project_id = p.id" +
                ") " +
                "WHERE p.deleted = 0 " +
                "AND p.like_count != (" +
                "    SELECT COUNT(*) FROM project_like pl " +
                "    WHERE pl.project_id = p.id" +
                ")");
    }

    /**
     * 校准 project.comment_count
     *
     * <p>只更新实际值与计数值不一致的行，减少写放大。
     */
    private int calibrateCommentCount() {
        return jdbcTemplate.update(
                "UPDATE project p " +
                "SET comment_count = (" +
                "    SELECT COUNT(*) FROM project_comment pc " +
                "    WHERE pc.project_id = p.id AND pc.deleted = 0" +
                ") " +
                "WHERE p.deleted = 0 " +
                "AND p.comment_count != (" +
                "    SELECT COUNT(*) FROM project_comment pc " +
                "    WHERE pc.project_id = p.id AND pc.deleted = 0" +
                ")");
    }

    /**
     * 校准 user.points
     *
     * <p>将 user.points 更新为 point_log 表中该用户的积分总和。
     * 只更新实际值与计算值不一致的行。
     */
    private int calibrateUserPoints() {
        return jdbcTemplate.update(
                "UPDATE user u " +
                "SET points = (" +
                "    SELECT COALESCE(SUM(pl.points), 0) FROM point_log pl " +
                "    WHERE pl.user_id = u.id" +
                ") " +
                "WHERE u.deleted = 0 " +
                "AND u.points != (" +
                "    SELECT COALESCE(SUM(pl.points), 0) FROM point_log pl " +
                "    WHERE pl.user_id = u.id" +
                ")");
    }
}
