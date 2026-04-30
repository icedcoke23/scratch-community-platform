-- ============================================================
-- V6: 用户邮箱字段 + 性能索引优化
-- ============================================================

-- 用户表新增邮箱字段（用于密码找回、通知等）
ALTER TABLE `user`
    ADD COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址' AFTER `bio`,
    ADD UNIQUE KEY `uk_email` (`email`);

-- 性能索引优化
-- 用户表：支持邮箱登录查询
CREATE INDEX idx_user_status ON `user` (`status`);

-- 项目表：支持热门排序（复合索引）
CREATE INDEX idx_project_status_like ON `project` (`status`, `like_count` DESC, `created_at` DESC);

-- 提交记录表：支持按用户+题目查询（竞赛排名场景）
CREATE INDEX idx_submission_user_problem ON `submission` (`user_id`, `problem_id`, `verdict`);

-- 积分记录表：支持按日期查询（每日签到检查）
CREATE INDEX idx_point_log_user_type_date ON `point_log` (`user_id`, `type`, `created_at`);

-- 作业提交表：支持按作业+状态查询（批改统计）
CREATE INDEX idx_homework_submission_homework_status ON `homework_submission` (`homework_id`, `status`);

-- 竞赛排名表：优化排序查询
CREATE INDEX idx_competition_ranking_score ON `competition_ranking` (`competition_id`, `total_score` DESC, `penalty` ASC);

-- 通知表：支持未读计数
CREATE INDEX idx_notification_user_unread ON `notification` (`user_id`, `is_read`, `created_at` DESC);

-- 审核记录表：支持待审核查询
CREATE INDEX idx_audit_log_pending ON `content_audit_log` (`status`, `created_at` DESC);
