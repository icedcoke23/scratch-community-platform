-- V9: v2.2 深度优化迁移
-- 数据库性能与数据完整性优化
-- 日期: 2026-04-24

-- ============================================================
-- 1. 竞赛报名唯一约束：防止重复报名
-- ============================================================
-- 先清理可能存在的重复数据（保留最新的报名记录）
DELETE cr1 FROM `competition_registration` cr1
INNER JOIN `competition_registration` cr2
ON cr1.competition_id = cr2.competition_id
AND cr1.user_id = cr2.user_id
AND cr1.id < cr2.id;

ALTER TABLE `competition_registration`
    ADD UNIQUE KEY `uk_competition_user` (`competition_id`, `user_id`);

-- ============================================================
-- 2. 提交记录性能索引：按用户+题目查询提交历史
-- ============================================================
CREATE INDEX `idx_submission_user_problem` ON `submission` (`user_id`, `problem_id`, `created_at` DESC);

-- ============================================================
-- 3. 积分日志性能索引：按用户查询积分历史
-- ============================================================
CREATE INDEX `idx_point_log_user` ON `point_log` (`user_id`, `created_at` DESC);

-- ============================================================
-- 4. AI 点评性能索引：按项目查询最新点评
-- ============================================================
CREATE INDEX `idx_ai_review_project` ON `ai_review` (`project_id`, `created_at` DESC);

-- ============================================================
-- 5. 竞赛排名性能索引
-- ============================================================
CREATE INDEX `idx_competition_ranking_score` ON `competition_ranking` (`competition_id`, `total_score` DESC);

-- ============================================================
-- 6. 作业提交性能索引：按作业查询提交列表
-- ============================================================
CREATE INDEX `idx_homework_submission_homework` ON `homework_submission` (`homework_id`, `created_at` DESC);

-- ============================================================
-- 7. 评论性能索引：按项目查询评论列表
-- ============================================================
CREATE INDEX `idx_project_comment_project` ON `project_comment` (`project_id`, `created_at` DESC);

-- ============================================================
-- 8. 审核日志性能索引：按内容查询审核历史
-- ============================================================
CREATE INDEX `idx_audit_log_content` ON `content_audit_log` (`content_type`, `content_id`, `created_at` DESC);
