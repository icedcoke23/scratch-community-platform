-- V12: 将 JSON ID 字段改为关联表

-- 1. homework 表的 problem_ids JSON 字段改为关联表
CREATE TABLE IF NOT EXISTS `homework_problem` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `homework_id` BIGINT NOT NULL,
    `problem_id` BIGINT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '题目顺序',
    `score` INT DEFAULT NULL COMMENT '该题分值',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_homework_problem` (`homework_id`, `problem_id`),
    KEY `idx_problem` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业-题目关联表';

-- 迁移现有 JSON 数据
INSERT INTO `homework_problem` (`homework_id`, `problem_id`, `sort_order`)
SELECT h.id, jt.problem_id, jt.sort_order
FROM `homework` h
CROSS JOIN JSON_TABLE(
    h.problem_ids,
    '$[*]' COLUMNS(problem_id BIGINT PATH '$', sort_order FOR ORDINALITY)
) jt
WHERE h.problem_ids IS NOT NULL AND JSON_LENGTH(h.problem_ids) > 0;

-- 2. competition 表的 problem_ids JSON 字段改为关联表
CREATE TABLE IF NOT EXISTS `competition_problem` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `competition_id` BIGINT NOT NULL,
    `problem_id` BIGINT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '题目顺序',
    `score` INT NOT NULL DEFAULT 100 COMMENT '该题分值',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_competition_problem` (`competition_id`, `problem_id`),
    KEY `idx_problem` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛-题目关联表';

-- 迁移现有数据
INSERT INTO `competition_problem` (`competition_id`, `problem_id`, `sort_order`, `score`)
SELECT c.id, jt.problem_id, jt.sort_order,
    COALESCE(
        JSON_EXTRACT(c.problem_scores, CONCAT('$[', jt.sort_order - 1, ']')),
        100
    ) AS score
FROM `competition` c
CROSS JOIN JSON_TABLE(
    c.problem_ids,
    '$[*]' COLUMNS(problem_id BIGINT PATH '$', sort_order FOR ORDINALITY)
) jt
WHERE c.problem_ids IS NOT NULL AND JSON_LENGTH(c.problem_ids) > 0;

-- 3. submission 表添加 competition_id 字段
ALTER TABLE `submission`
    ADD COLUMN `competition_id` BIGINT DEFAULT NULL COMMENT '竞赛 ID（普通提交为 NULL）' AFTER `problem_id`,
    ADD KEY `idx_competition` (`competition_id`);
