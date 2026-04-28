-- V19: 补充缺失索引 + notification 表优化
-- 基于全面优化分析报告

-- 1. point_log: 用户积分历史查询 (user_id + created_at DESC)
CREATE INDEX IF NOT EXISTS `idx_point_log_user_time`
    ON `point_log` (`user_id`, `created_at` DESC);

-- 2. point_log: 按类型查询积分记录 (user_id + type + created_at)
-- 用于 getTodayPointsByType() 和 hasCheckedInToday() 查询优化
CREATE INDEX IF NOT EXISTS `idx_point_log_user_type_time`
    ON `point_log` (`user_id`, `type`, `created_at`);

-- 3. notification: 按时间排序的通知列表 (user_id + created_at DESC)
CREATE INDEX IF NOT EXISTS `idx_notification_user_time`
    ON `notification` (`user_id`, `created_at` DESC);

-- 4. project: 用户项目列表查询 (user_id + status + created_at DESC)
CREATE INDEX IF NOT EXISTS `idx_project_user_status_time`
    ON `project` (`user_id`, `status`, `created_at` DESC);

-- 5. notification: 过期通知清理查询优化 (is_read + created_at)
-- 用于定时任务清理 90 天已读通知和 180 天未读通知
CREATE INDEX IF NOT EXISTS `idx_notification_read_created`
    ON `notification` (`is_read`, `created_at`);
