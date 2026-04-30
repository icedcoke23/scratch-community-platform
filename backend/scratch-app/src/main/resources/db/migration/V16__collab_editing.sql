-- V16: 协作编辑表
-- 支持多人实时协作编辑 Scratch 项目

CREATE TABLE IF NOT EXISTS `collab_session` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `owner_id` BIGINT NOT NULL COMMENT '创建者 ID',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '会话状态: active / closed',
    `max_editors` INT DEFAULT 5 COMMENT '最大编辑者数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX `idx_project_id` (`project_id`),
    INDEX `idx_owner_id` (`owner_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='协作编辑会话表';

CREATE TABLE IF NOT EXISTS `collab_participant` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL COMMENT '会话 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `role` VARCHAR(20) DEFAULT 'viewer' COMMENT '角色: editor / viewer',
    `cursor_x` INT DEFAULT 0 COMMENT '光标 X 坐标',
    `cursor_y` INT DEFAULT 0 COMMENT '光标 Y 坐标',
    `last_active_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',

    UNIQUE KEY `uk_session_user` (`session_id`, `user_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='协作参与者表';
