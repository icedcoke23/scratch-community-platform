-- V15: 用户第三方登录绑定表
-- 支持微信、QQ 等第三方平台登录

CREATE TABLE IF NOT EXISTS `user_oauth` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `provider` VARCHAR(20) NOT NULL COMMENT '第三方平台: wechat / qq',
    `open_id` VARCHAR(128) NOT NULL COMMENT '第三方平台的唯一标识',
    `union_id` VARCHAR(128) DEFAULT NULL COMMENT 'Union ID（微信开放平台跨应用识别）',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '第三方平台返回的昵称',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '第三方平台返回的头像',
    `access_token` VARCHAR(500) DEFAULT NULL COMMENT 'Access Token（短期有效）',
    `refresh_token` VARCHAR(500) DEFAULT NULL COMMENT 'Refresh Token',
    `token_expires_at` DATETIME DEFAULT NULL COMMENT 'Token 过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    UNIQUE KEY `uk_provider_openid` (`provider`, `open_id`) COMMENT '同一平台的 open_id 唯一',
    INDEX `idx_user_id` (`user_id`) COMMENT '按用户查询绑定',
    INDEX `idx_union_id` (`union_id`) COMMENT '按 union_id 关联',
    INDEX `idx_provider` (`provider`) COMMENT '按平台查询',

    -- 外键（逻辑约束，不实际创建外键以保持灵活性）
    INDEX `idx_user_provider` (`user_id`, `provider`) COMMENT '用户+平台联合查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户第三方登录绑定表';

-- 添加第三方登录来源标记到 user 表
ALTER TABLE `user`
    ADD COLUMN `oauth_source` VARCHAR(20) DEFAULT NULL COMMENT '注册来源（第三方平台名，NULL 表示本地注册）' AFTER `bio`;
