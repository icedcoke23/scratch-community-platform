-- V8: v2.1 优化迁移
-- 为 user 表添加登录追踪字段，为 notification 表添加性能索引

-- 1. user 表添加 last_login_at（最后登录时间）和 login_count（登录次数）
ALTER TABLE `user` ADD COLUMN `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间';
ALTER TABLE `user` ADD COLUMN `login_count` INT NOT NULL DEFAULT 0 COMMENT '登录次数';

-- 2. notification 表添加 (user_id, type) 复合索引，优化按用户+类型查询通知
CREATE INDEX `idx_notification_user_type` ON `notification` (`user_id`, `type`);
