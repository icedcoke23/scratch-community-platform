-- ============================================================
-- Sprint 6: 教室模块新增表
-- 请在 init.sql 末尾追加以下内容
-- ============================================================

-- 作业表
CREATE TABLE IF NOT EXISTS `homework` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT NOT NULL COMMENT '班级 ID',
    `teacher_id` BIGINT NOT NULL COMMENT '教师 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '作业标题',
    `description` TEXT DEFAULT NULL COMMENT '作业描述',
    `type` VARCHAR(30) NOT NULL DEFAULT 'scratch_project' COMMENT '类型: scratch_project/choice/mixed',
    `problem_ids` JSON DEFAULT NULL COMMENT '关联题目 ID 列表',
    `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
    `total_score` INT NOT NULL DEFAULT 100 COMMENT '满分',
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published/closed',
    `submit_count` INT NOT NULL DEFAULT 0 COMMENT '提交人数',
    `graded_count` INT NOT NULL DEFAULT 0 COMMENT '已批改人数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_teacher` (`teacher_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

-- 作业提交表
CREATE TABLE IF NOT EXISTS `homework_submission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `homework_id` BIGINT NOT NULL COMMENT '作业 ID',
    `student_id` BIGINT NOT NULL COMMENT '学生 ID',
    `project_id` BIGINT DEFAULT NULL COMMENT '提交的项目 ID',
    `answers` JSON DEFAULT NULL COMMENT '选择题答案',
    `score` INT DEFAULT NULL COMMENT '得分',
    `comment` TEXT DEFAULT NULL COMMENT '教师评语',
    `status` VARCHAR(20) NOT NULL DEFAULT 'submitted' COMMENT '状态: submitted/graded/returned',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `graded_at` DATETIME DEFAULT NULL COMMENT '批改时间',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_homework_student` (`homework_id`, `student_id`),
    KEY `idx_student` (`student_id`),
    KEY `idx_homework` (`homework_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';
