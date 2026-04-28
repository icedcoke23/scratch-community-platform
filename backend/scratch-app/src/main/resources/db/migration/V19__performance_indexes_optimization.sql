-- ============================================================
-- V19: 性能优化 — 补充缺失索引
-- Scratch Community Platform
-- ============================================================

-- point_log: 今日积分查询 (user_id, type, created_at)
-- 被 CrossModuleQueryRepository.getTodayPointsByType() 高频调用
CREATE INDEX idx_point_user_type_date ON point_log (user_id, type, created_at);

-- point_log: 用户积分时间线
CREATE INDEX idx_point_user_date ON point_log (user_id, created_at);

-- notification: 通知列表按时间排序
CREATE INDEX idx_notification_user_time ON notification (user_id, created_at DESC);

-- project: 用户项目列表优化 (user_id, status, created_at)
-- 被 myProjects() 和 getPublishedProjectsByPopularity() 使用
CREATE INDEX idx_project_user_status_time ON project (user_id, status, created_at DESC);
