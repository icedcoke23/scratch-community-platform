-- V17: JWT Refresh Token 支持 + 补充索引

-- 1. user 表增加 refresh_token 相关字段
ALTER TABLE `user`
    ADD COLUMN `refresh_token` VARCHAR(512) DEFAULT NULL COMMENT '当前有效 Refresh Token' AFTER `login_count`,
    ADD COLUMN `refresh_token_expires_at` DATETIME DEFAULT NULL COMMENT 'Refresh Token 过期时间' AFTER `refresh_token`;

-- 2. 补充缺失索引（使用 IF NOT EXISTS 防止重复创建）
-- submission 表: 按用户查询最近提交
CREATE INDEX IF NOT EXISTS `idx_submission_user_created` ON `submission` (`user_id`, `created_at` DESC);

-- project 表: 按用户查询已发布项目
CREATE INDEX IF NOT EXISTS `idx_project_user_status_created` ON `project` (`user_id`, `status`, `created_at` DESC);

-- user_follow 表: 查询关注列表
CREATE INDEX IF NOT EXISTS `idx_user_follow_following` ON `user_follow` (`following_id`, `created_at` DESC);

-- user_follow 表: 查询粉丝列表
CREATE INDEX IF NOT EXISTS `idx_user_follow_follower` ON `user_follow` (`follower_id`, `created_at` DESC);

-- collab_session 表: 按项目查询协作会话
CREATE INDEX IF NOT EXISTS `idx_collab_session_project` ON `collab_session` (`project_id`, `status`);

-- collab_participant 表: 按用户查询参与的协作
CREATE INDEX IF NOT EXISTS `idx_collab_participant_user` ON `collab_participant` (`user_id`, `joined_at`);
