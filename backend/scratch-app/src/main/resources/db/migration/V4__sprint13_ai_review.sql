-- ============================================================
-- V4: Sprint 13 — AI 点评系统
-- ============================================================

-- AI 点评记录表
CREATE TABLE IF NOT EXISTS `ai_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `user_id` BIGINT NOT NULL COMMENT '触发用户 ID',
    `overall_score` TINYINT NOT NULL COMMENT '总体评分 (1-5)',
    `dimension_scores` JSON COMMENT '各维度评分',
    `summary` VARCHAR(500) COMMENT '总结评语',
    `detail` TEXT COMMENT '详细点评 (Markdown)',
    `strengths` JSON COMMENT '优点列表',
    `suggestions` JSON COMMENT '改进建议',
    `block_count` INT COMMENT '分析的积木数',
    `sprite_count` INT COMMENT '分析的角色数',
    `complexity_score` DOUBLE COMMENT '复杂度评分',
    `provider` VARCHAR(10) NOT NULL DEFAULT 'RULE' COMMENT '生成方式: RULE/LLM',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_project_time` (`project_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 点评记录表';
