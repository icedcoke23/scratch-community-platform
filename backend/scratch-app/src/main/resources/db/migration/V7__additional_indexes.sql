-- ============================================================
-- V7: 补充性能索引（与 V6 互补）
-- Scratch Community Platform - 全面优化
-- ============================================================

-- 1. project 表: Feed 流覆盖索引 (status + deleted + created_at)
-- FeedService.getFeed() 查询优化
CREATE INDEX IF NOT EXISTS `idx_project_status_deleted_created` ON `project` (`status`, `deleted`, `created_at`);

-- 2. project_like 表: 批量点赞状态查询
-- FeedService 中批量查询用户是否已点赞
CREATE INDEX IF NOT EXISTS `idx_project_like_user_project` ON `project_like` (`user_id`, `project_id`);

-- 3. homework_submission 表: 按作业+学生查询
CREATE INDEX IF NOT EXISTS `idx_homework_sub_homework_student` ON `homework_submission` (`homework_id`, `student_id`);

-- 4. user 表: 积分排行查询
CREATE INDEX IF NOT EXISTS `idx_user_points` ON `user` (`points` DESC, `status`, `deleted`);
