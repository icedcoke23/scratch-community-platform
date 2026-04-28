-- ============================================================
-- V5: Sprint 14 — 竞赛系统
-- ============================================================

-- 竞赛表
CREATE TABLE IF NOT EXISTS `competition` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '竞赛标题',
    `description` TEXT COMMENT '竞赛描述',
    `creator_id` BIGINT NOT NULL COMMENT '创建者 ID',
    `type` VARCHAR(20) NOT NULL DEFAULT 'TIMED' COMMENT '竞赛类型: TIMED/RATED',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `problem_ids` JSON NOT NULL COMMENT '题目 ID 列表',
    `problem_scores` JSON COMMENT '每题分值',
    `total_score` INT NOT NULL DEFAULT 100 COMMENT '总分',
    `participant_count` INT NOT NULL DEFAULT 0 COMMENT '参赛人数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/RUNNING/ENDED',
    `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛表';

-- 竞赛报名表
CREATE TABLE IF NOT EXISTS `competition_registration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `competition_id` BIGINT NOT NULL COMMENT '竞赛 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_competition_user` (`competition_id`, `user_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛报名表';

-- 竞赛排名表
CREATE TABLE IF NOT EXISTS `competition_ranking` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `competition_id` BIGINT NOT NULL COMMENT '竞赛 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `total_score` INT NOT NULL DEFAULT 0 COMMENT '总得分',
    `solved_count` INT NOT NULL DEFAULT 0 COMMENT '通过题目数',
    `penalty` INT NOT NULL DEFAULT 0 COMMENT '总罚时（分钟）',
    `rank` INT DEFAULT NULL COMMENT '排名',
    `problem_details` JSON COMMENT '各题得分详情',
    `last_submit_time` DATETIME COMMENT '最后提交时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_competition_user` (`competition_id`, `user_id`),
    KEY `idx_ranking` (`competition_id`, `total_score` DESC, `penalty` ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛排名表';
