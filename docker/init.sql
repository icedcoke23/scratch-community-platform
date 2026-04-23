-- ============================================================
-- Scratch Community Platform - 数据库初始化脚本
-- 基于核心表设计 v0.2 (18 张核心表)
-- ============================================================

CREATE DATABASE IF NOT EXISTS scratch_community
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE scratch_community;

-- ============================================================
-- 1. 用户模块
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码 (BCrypt)',
    `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
    `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色: STUDENT/TEACHER/ADMIN',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 关注关系表
CREATE TABLE IF NOT EXISTS `user_follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `follower_id` BIGINT NOT NULL COMMENT '关注者 ID',
    `following_id` BIGINT NOT NULL COMMENT '被关注者 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow` (`follower_id`, `following_id`),
    KEY `idx_following` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系表';

-- 班级表
CREATE TABLE IF NOT EXISTS `class` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '班级名称',
    `teacher_id` BIGINT NOT NULL COMMENT '教师 ID',
    `invite_code` VARCHAR(20) NOT NULL COMMENT '邀请码',
    `grade` VARCHAR(50) DEFAULT NULL COMMENT '年级',
    `student_count` INT NOT NULL DEFAULT 0 COMMENT '学生人数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invite_code` (`invite_code`),
    KEY `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 班级学生关系表
CREATE TABLE IF NOT EXISTS `class_student` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_student` (`class_id`, `student_id`),
    KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学生关系表';

-- ============================================================
-- 2. 创作模块
-- ============================================================

-- 项目表
CREATE TABLE IF NOT EXISTS `project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '作者 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '项目标题',
    `description` TEXT DEFAULT NULL COMMENT '项目描述',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面 URL',
    `sb3_url` VARCHAR(500) DEFAULT NULL COMMENT 'sb3 文件 URL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published',
    `block_count` INT DEFAULT 0 COMMENT '积木数量',
    `complexity_score` DOUBLE DEFAULT 0 COMMENT '复杂度评分',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    `parse_result` JSON DEFAULT NULL COMMENT 'sb3 解析结果 (JSON)',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签 (逗号分隔)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created` (`created_at`),
    FULLTEXT KEY `ft_search` (`title`, `description`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ============================================================
-- 3. 社区模块
-- ============================================================

-- 点赞表
CREATE TABLE IF NOT EXISTS `project_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `project_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_project` (`user_id`, `project_id`),
    KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- 评论表
CREATE TABLE IF NOT EXISTS `project_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '评论者 ID',
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ============================================================
-- 4. 判题模块
-- ============================================================

-- 题目表
CREATE TABLE IF NOT EXISTS `problem` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '题目标题',
    `description` TEXT NOT NULL COMMENT '题目描述',
    `type` VARCHAR(30) NOT NULL COMMENT '题型: scratch_algo/choice/true_false',
    `difficulty` VARCHAR(20) NOT NULL DEFAULT 'easy' COMMENT '难度: easy/medium/hard',
    `score` INT NOT NULL DEFAULT 100 COMMENT '分值',
    `time_limit` INT NOT NULL DEFAULT 30000 COMMENT '时间限制 (ms)',
    `memory_limit` INT NOT NULL DEFAULT 512 COMMENT '内存限制 (MB)',
    `test_cases` JSON DEFAULT NULL COMMENT '测试用例 [{input, expectedOutput}]',
    `answer` VARCHAR(500) DEFAULT NULL COMMENT '选择题答案',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签',
    `ac_count` INT NOT NULL DEFAULT 0 COMMENT '通过次数',
    `submit_count` INT NOT NULL DEFAULT 0 COMMENT '提交次数',
    `creator_id` BIGINT NOT NULL COMMENT '创建者 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_difficulty` (`difficulty`),
    FULLTEXT KEY `ft_search` (`title`, `description`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 提交记录表
CREATE TABLE IF NOT EXISTS `submission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '提交者 ID',
    `problem_id` BIGINT NOT NULL COMMENT '题目 ID',
    `answer` VARCHAR(500) DEFAULT NULL COMMENT '选择题答案',
    `sb3_url` VARCHAR(500) DEFAULT NULL COMMENT 'sb3 文件 URL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/AC/WA/TLE/RE',
    `score` INT DEFAULT 0 COMMENT '得分',
    `time_used` INT DEFAULT NULL COMMENT '用时 (ms)',
    `details` JSON DEFAULT NULL COMMENT '判题详情',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_problem` (`problem_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交记录表';

-- ============================================================
-- 5. 教室模块
-- ============================================================

-- 作业表
CREATE TABLE IF NOT EXISTS `homework` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT NOT NULL COMMENT '班级 ID',
    `teacher_id` BIGINT NOT NULL COMMENT '教师 ID',
    `title` VARCHAR(200) NOT NULL COMMENT '作业标题',
    `description` TEXT DEFAULT NULL COMMENT '作业描述',
    `problem_id` BIGINT DEFAULT NULL COMMENT '关联题目 ID (可选)',
    `deadline` DATETIME NOT NULL COMMENT '截止时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_class` (`class_id`),
    KEY `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

-- 作业提交表
CREATE TABLE IF NOT EXISTS `homework_submission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `homework_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `sb3_url` VARCHAR(500) DEFAULT NULL COMMENT 'sb3 文件 URL',
    `parse_result` JSON DEFAULT NULL COMMENT 'sb3 解析结果',
    `auto_score` INT DEFAULT NULL COMMENT '自动评分 (关联题目时)',
    `teacher_score` INT DEFAULT NULL COMMENT '教师评分',
    `teacher_comment` TEXT DEFAULT NULL COMMENT '教师评语',
    `final_score` INT DEFAULT NULL COMMENT '最终得分',
    `status` VARCHAR(20) NOT NULL DEFAULT 'submitted' COMMENT 'submitted/graded',
    `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `graded_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_homework_student` (`homework_id`, `student_id`),
    KEY `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';

-- ============================================================
-- 6. 系统模块
-- ============================================================

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '接收者 ID',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT DEFAULT NULL COMMENT '内容',
    `data` JSON DEFAULT NULL COMMENT '附加数据',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 审核记录表
CREATE TABLE IF NOT EXISTS `content_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content_type` VARCHAR(50) NOT NULL COMMENT '内容类型: project/comment/homework',
    `content_id` BIGINT NOT NULL COMMENT '内容 ID',
    `content_text` TEXT COMMENT '审核内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/passed/rejected',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
    `operator_id` BIGINT DEFAULT NULL COMMENT '审核人 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_content` (`content_type`, `content_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核记录表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT NOT NULL COMMENT '配置值',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 默认管理员 (密码: admin123, BCrypt 加密)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2b$10$y3vcNmGVf0Y903xNnhr1IefOOJsmOdTr1QqzH7txQMBIPMDmMqa76', '管理员', 'ADMIN');

-- 默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('site.name', 'Scratch 社区', '站点名称'),
('site.description', '面向 K12 课后教学的 Scratch 编程社区', '站点描述'),
('register.enabled', 'true', '是否开放注册'),
('upload.max_size', '52428800', '上传文件大小限制 (bytes)');
