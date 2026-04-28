-- H2 测试用 schema（从 init.sql 精简，兼容 H2 MODE=MYSQL）

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(200) NOT NULL,
    `nickname` VARCHAR(50),
    `email` VARCHAR(100),
    `avatar_url` VARCHAR(500),
    `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    `status` INT NOT NULL DEFAULT 1,
    `points` INT NOT NULL DEFAULT 0,
    `level` INT NOT NULL DEFAULT 1,
    `last_login_at` TIMESTAMP,
    `login_count` INT NOT NULL DEFAULT 0,
    `refresh_token` VARCHAR(512),
    `refresh_token_expires_at` TIMESTAMP,
    `bio` VARCHAR(500),
    `version` INT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `user_follow` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `follow_user_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (`user_id`, `follow_user_id`)
);

CREATE TABLE IF NOT EXISTS `class` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500),
    `teacher_id` BIGINT NOT NULL,
    `invite_code` VARCHAR(20) NOT NULL UNIQUE,
    `student_count` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'active',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `class_student` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (`class_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `project` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` VARCHAR(1000),
    `cover_url` VARCHAR(500),
    `sb3_url` VARCHAR(500),
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft',
    `block_count` INT DEFAULT 0,
    `complexity_score` DOUBLE DEFAULT 0,
    `like_count` INT NOT NULL DEFAULT 0,
    `comment_count` INT NOT NULL DEFAULT 0,
    `view_count` INT NOT NULL DEFAULT 0,
    `remix_project_id` BIGINT,
    `remix_count` INT NOT NULL DEFAULT 0,
    `parse_result` TEXT,
    `tags` VARCHAR(500),
    `version` INT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `project_like` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `project_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (`user_id`, `project_id`)
);

CREATE TABLE IF NOT EXISTS `project_comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `project_id` BIGINT NOT NULL,
    `content` VARCHAR(1000) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `problem` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `difficulty` VARCHAR(20) DEFAULT 'medium',
    `description` TEXT,
    `answer` VARCHAR(500),
    `sb3_url` VARCHAR(500),
    `test_cases` TEXT,
    `time_limit` INT DEFAULT 30000,
    `memory_limit` INT DEFAULT 256,
    `submit_count` INT NOT NULL DEFAULT 0,
    `accept_count` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft',
    `creator_id` BIGINT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `submission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `problem_id` BIGINT NOT NULL,
    `competition_id` BIGINT DEFAULT NULL,
    `answer` VARCHAR(2000),
    `sb3_url` VARCHAR(500),
    `verdict` VARCHAR(20),
    `judge_detail` TEXT,
    `time_used` INT,
    `memory_used` INT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `homework` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_id` BIGINT NOT NULL,
    `teacher_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` VARCHAR(1000),
    `type` VARCHAR(20) NOT NULL DEFAULT 'scratch_project',
    `total_score` INT NOT NULL DEFAULT 100,
    `deadline` TIMESTAMP,
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft',
    `submit_count` INT NOT NULL DEFAULT 0,
    `graded_count` INT NOT NULL DEFAULT 0,
    `version` INT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `homework_submission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `homework_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `project_id` BIGINT,
    `content` VARCHAR(2000),
    `score` INT,
    `comment` VARCHAR(500),
    `status` VARCHAR(20) NOT NULL DEFAULT 'submitted',
    `submitted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `graded_at` TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(30) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` VARCHAR(1000),
    `data` VARCHAR(500),
    `is_read` INT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `content_audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `target_type` VARCHAR(30) NOT NULL,
    `target_id` BIGINT NOT NULL,
    `action` VARCHAR(30) NOT NULL,
    `reason` VARCHAR(500),
    `operator_id` BIGINT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `config_key` VARCHAR(100) NOT NULL UNIQUE,
    `config_value` VARCHAR(2000),
    `description` VARCHAR(500),
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `point_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `action` VARCHAR(30) NOT NULL,
    `points` INT NOT NULL,
    `source_id` BIGINT,
    `description` VARCHAR(200),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `competition` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `description` VARCHAR(1000),
    `creator_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL DEFAULT 'TIMED',
    `start_time` TIMESTAMP,
    `end_time` TIMESTAMP,
    `problem_ids` VARCHAR(500),
    `problem_scores` VARCHAR(500),
    `total_score` INT NOT NULL DEFAULT 100,
    `participant_count` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `is_public` INT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `competition_registration` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `competition_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `registered_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (`competition_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `competition_ranking` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `competition_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `total_score` INT NOT NULL DEFAULT 0,
    `solved_count` INT NOT NULL DEFAULT 0,
    `penalty` INT NOT NULL DEFAULT 0,
    `rank` INT,
    `problem_details` TEXT,
    `version` INT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (`competition_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `ai_review` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `overall_score` INT NOT NULL,
    `dimension_scores` VARCHAR(500),
    `summary` VARCHAR(500),
    `detail` TEXT,
    `strengths` VARCHAR(1000),
    `suggestions` VARCHAR(1000),
    `block_count` INT,
    `sprite_count` INT,
    `complexity_score` DOUBLE,
    `provider` VARCHAR(50) DEFAULT 'RULE',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========== V12: 关联表 ==========

CREATE TABLE IF NOT EXISTS `homework_problem` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `homework_id` BIGINT NOT NULL,
    `problem_id` BIGINT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `score` INT DEFAULT NULL,
    UNIQUE (`homework_id`, `problem_id`)
);

CREATE TABLE IF NOT EXISTS `competition_problem` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `competition_id` BIGINT NOT NULL,
    `problem_id` BIGINT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `score` INT NOT NULL DEFAULT 100,
    UNIQUE (`competition_id`, `problem_id`)
);

-- ========== V13: 性能索引 ==========

CREATE INDEX IF NOT EXISTS `idx_homework_problem_homework` ON `homework_problem` (`homework_id`, `sort_order`);
CREATE INDEX IF NOT EXISTS `idx_competition_problem_competition` ON `competition_problem` (`competition_id`, `sort_order`);
CREATE INDEX IF NOT EXISTS `idx_submission_competition_user` ON `submission` (`competition_id`, `user_id`, `verdict`);
CREATE INDEX IF NOT EXISTS `idx_point_log_checkin` ON `point_log` (`user_id`, `action`, `created_at`);
CREATE INDEX IF NOT EXISTS `idx_ai_review_project_latest` ON `ai_review` (`project_id`, `created_at` DESC);
CREATE INDEX IF NOT EXISTS `idx_competition_status_time` ON `competition` (`status`, `start_time`, `end_time`);
