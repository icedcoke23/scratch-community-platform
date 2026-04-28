-- ============================================================
-- Scratch Community Platform - 数据库初始化脚本
-- 与 Flyway V1-V14 迁移脚本对齐
-- 包含全部 21 张核心表（含 2 张关联表）
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
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `oauth_source` VARCHAR(20) DEFAULT NULL COMMENT '注册来源（第三方平台名，NULL 表示本地注册）',
    `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色: STUDENT/TEACHER/ADMIN',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    `points` INT NOT NULL DEFAULT 0 COMMENT '总积分',
    `level` INT NOT NULL DEFAULT 1 COMMENT '等级: 1-8',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `login_count` INT NOT NULL DEFAULT 0 COMMENT '登录次数',
    `refresh_token` VARCHAR(512) DEFAULT NULL COMMENT '当前有效 Refresh Token',
    `refresh_token_expires_at` DATETIME DEFAULT NULL COMMENT 'Refresh Token 过期时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_nickname` (`nickname`),
    KEY `idx_points` (`points`),
    KEY `idx_level` (`level`),
    KEY `idx_user_status` (`status`),
    KEY `idx_user_points_rank` (`points` DESC, `status`, `deleted`)
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
    `description` VARCHAR(500) DEFAULT NULL COMMENT '班级描述',
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
    `remix_project_id` BIGINT DEFAULT NULL COMMENT '原始项目 ID（Remix 来源，NULL=原创）',
    `remix_count` INT NOT NULL DEFAULT 0 COMMENT 'Remix 次数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created` (`created_at`),
    KEY `idx_remix_project` (`remix_project_id`),
    KEY `idx_project_status_like` (`status`, `like_count` DESC, `created_at` DESC),
    KEY `idx_project_feed` (`status`, `deleted`, `created_at`),
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
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签',
    `options` JSON DEFAULT NULL COMMENT '选择题选项 [{"key":"A","text":"..."}]',
    `answer` VARCHAR(500) DEFAULT NULL COMMENT '选择题/判断题答案',
    `expected_output` JSON DEFAULT NULL COMMENT '编程题预期输出',
    `template_sb3_url` VARCHAR(500) DEFAULT NULL COMMENT '编程题模板 sb3 URL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft/published',
    `submit_count` INT NOT NULL DEFAULT 0 COMMENT '提交次数',
    `accept_count` INT NOT NULL DEFAULT 0 COMMENT '通过次数',
    `creator_id` BIGINT NOT NULL COMMENT '创建者 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`),
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_status` (`status`),
    FULLTEXT KEY `ft_search` (`title`, `description`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 提交记录表
CREATE TABLE IF NOT EXISTS `submission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '提交者 ID',
    `problem_id` BIGINT NOT NULL COMMENT '题目 ID',
    `competition_id` BIGINT DEFAULT NULL COMMENT '竞赛 ID（普通提交为 NULL）',
    `submit_type` VARCHAR(30) NOT NULL COMMENT '提交类型: sb3/choice/true_false',
    `answer` VARCHAR(500) DEFAULT NULL COMMENT '选择题答案',
    `sb3_url` VARCHAR(500) DEFAULT NULL COMMENT 'sb3 文件 URL',
    `verdict` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '判题结果: PENDING/AC/WA/TLE/RE',
    `judge_detail` TEXT DEFAULT NULL COMMENT '判题详情 JSON',
    `runtime_ms` BIGINT DEFAULT NULL COMMENT '运行耗时 (ms)',
    `memory_kb` BIGINT DEFAULT NULL COMMENT '运行内存 (KB)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_problem` (`problem_id`),
    KEY `idx_verdict` (`verdict`),
    KEY `idx_submission_user_problem` (`user_id`, `problem_id`, `verdict`)
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
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
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
    KEY `idx_homework` (`homework_id`),
    KEY `idx_homework_sub_homework_status` (`homework_id`, `status`),
    KEY `idx_homework_sub_homework_student` (`homework_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';

-- ============================================================
-- 6. 积分模块
-- ============================================================

-- 积分变动记录表
CREATE TABLE IF NOT EXISTS `point_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `type` VARCHAR(30) NOT NULL COMMENT '变动类型: DAILY_CHECKIN/PUBLISH_PROJECT/RECEIVE_LIKE/AC_SUBMISSION/COMPLETE_HOMEWORK/ADMIN_ADJUST',
    `points` INT NOT NULL COMMENT '变动积分（正数增加，负数扣减）',
    `total_points` INT NOT NULL DEFAULT 0 COMMENT '变动后总积分',
    `ref_type` VARCHAR(30) DEFAULT NULL COMMENT '关联对象类型: project/submission/homework/user',
    `ref_id` BIGINT DEFAULT NULL COMMENT '关联对象 ID',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_user_type_date` (`user_id`, `type`, `created_at`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动记录表';

-- ============================================================
-- 7. 竞赛模块
-- ============================================================

-- 竞赛表
CREATE TABLE IF NOT EXISTS `competition` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '竞赛标题',
    `description` TEXT DEFAULT NULL COMMENT '竞赛描述',
    `creator_id` BIGINT NOT NULL COMMENT '创建者 ID',
    `type` VARCHAR(20) NOT NULL DEFAULT 'TIMED' COMMENT '类型: TIMED/RATED',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `problem_ids` JSON DEFAULT NULL COMMENT '题目 ID 列表',
    `problem_scores` JSON DEFAULT NULL COMMENT '每题分值',
    `total_score` INT NOT NULL DEFAULT 100 COMMENT '总分',
    `participant_count` INT NOT NULL DEFAULT 0 COMMENT '参赛人数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/RUNNING/ENDED',
    `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
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
    `problem_details` JSON DEFAULT NULL COMMENT '各题得分详情',
    `last_submit_time` DATETIME DEFAULT NULL COMMENT '最后提交时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_competition_user` (`competition_id`, `user_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_competition_rank` (`competition_id`, `rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛排名表';

-- ============================================================
-- 7.5 关联表（V12 迁移）
-- ============================================================

-- 作业-题目关联表
CREATE TABLE IF NOT EXISTS `homework_problem` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `homework_id` BIGINT NOT NULL,
    `problem_id` BIGINT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '题目顺序',
    `score` INT DEFAULT NULL COMMENT '该题分值',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_homework_problem` (`homework_id`, `problem_id`),
    KEY `idx_problem` (`problem_id`),
    KEY `idx_homework_problem_homework` (`homework_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业-题目关联表';

-- 竞赛-题目关联表
CREATE TABLE IF NOT EXISTS `competition_problem` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `competition_id` BIGINT NOT NULL,
    `problem_id` BIGINT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '题目顺序',
    `score` INT NOT NULL DEFAULT 100 COMMENT '该题分值',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_competition_problem` (`competition_id`, `problem_id`),
    KEY `idx_problem` (`problem_id`),
    KEY `idx_competition_problem_competition` (`competition_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞赛-题目关联表';

-- ============================================================
-- 8. AI 点评模块
-- ============================================================

-- AI 点评记录表
CREATE TABLE IF NOT EXISTS `ai_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `user_id` BIGINT NOT NULL COMMENT '触发用户 ID',
    `overall_score` INT NOT NULL DEFAULT 0 COMMENT '总体评分 (1-5)',
    `dimension_scores` JSON DEFAULT NULL COMMENT '各维度评分 JSON',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '总结评语',
    `detail` TEXT DEFAULT NULL COMMENT '详细点评 (Markdown)',
    `strengths` JSON DEFAULT NULL COMMENT '优点列表 (JSON 数组)',
    `suggestions` JSON DEFAULT NULL COMMENT '改进建议 (JSON 数组)',
    `block_count` INT DEFAULT 0 COMMENT '分析的积木数',
    `sprite_count` INT DEFAULT 0 COMMENT '分析的角色数',
    `complexity_score` DOUBLE DEFAULT 0 COMMENT '复杂度评分',
    `provider` VARCHAR(20) NOT NULL DEFAULT 'RULE' COMMENT '生成方式: RULE/LLM',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_project_created` (`project_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 点评记录表';

-- ============================================================
-- 9. 系统模块
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
    KEY `idx_notification_user_unread` (`user_id`, `is_read`, `created_at` DESC),
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
    KEY `idx_status` (`status`),
    KEY `idx_audit_log_pending` (`status`, `created_at` DESC)
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
-- 用户第三方登录绑定表（V15 迁移）
-- ============================================================
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
    UNIQUE KEY `uk_provider_openid` (`provider`, `open_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_union_id` (`union_id`),
    INDEX `idx_provider` (`provider`),
    INDEX `idx_user_provider` (`user_id`, `provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户第三方登录绑定表';

-- ============================================================
-- 协作编辑表（V16 迁移）
-- ============================================================
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

-- ============================================================
-- 性能索引（V9/V13/V17/V18 迁移）
-- ============================================================

-- submission 竞赛查询索引
CREATE INDEX IF NOT EXISTS `idx_submission_competition_user` ON `submission` (`competition_id`, `user_id`, `verdict`);

-- point_log 每日签到检查优化
CREATE INDEX IF NOT EXISTS `idx_point_log_checkin` ON `point_log` (`user_id`, `type`, `created_at`);

-- ai_review 按项目查询最新
CREATE INDEX IF NOT EXISTS `idx_ai_review_project_latest` ON `ai_review` (`project_id`, `created_at` DESC);

-- competition 按状态+时间查询
CREATE INDEX IF NOT EXISTS `idx_competition_status_time` ON `competition` (`status`, `start_time`, `end_time`);

-- V9: 竞赛报名唯一约束
CREATE UNIQUE INDEX IF NOT EXISTS `uk_competition_user` ON `competition_registration` (`competition_id`, `user_id`);

-- V9: 积分日志性能索引
CREATE INDEX IF NOT EXISTS `idx_point_log_user` ON `point_log` (`user_id`, `created_at` DESC);

-- V9: AI 点评性能索引
CREATE INDEX IF NOT EXISTS `idx_ai_review_project` ON `ai_review` (`project_id`, `created_at` DESC);

-- V9: 竞赛排名性能索引
CREATE INDEX IF NOT EXISTS `idx_competition_ranking_score` ON `competition_ranking` (`competition_id`, `total_score` DESC);

-- V9: 作业提交性能索引
CREATE INDEX IF NOT EXISTS `idx_homework_submission_homework` ON `homework_submission` (`homework_id`, `created_at` DESC);

-- V9: 评论性能索引
CREATE INDEX IF NOT EXISTS `idx_project_comment_project` ON `project_comment` (`project_id`, `created_at` DESC);

-- V9: 审核日志性能索引
CREATE INDEX IF NOT EXISTS `idx_audit_log_content` ON `content_audit_log` (`content_type`, `content_id`, `created_at` DESC);

-- V13: homework_problem 索引
CREATE INDEX IF NOT EXISTS `idx_homework_problem_homework` ON `homework_problem` (`homework_id`, `sort_order`);

-- V13: competition_problem 索引
CREATE INDEX IF NOT EXISTS `idx_competition_problem_competition` ON `competition_problem` (`competition_id`, `sort_order`);

-- V17: submission 按用户查询最近提交
CREATE INDEX IF NOT EXISTS `idx_submission_user_created` ON `submission` (`user_id`, `created_at` DESC);

-- V17: project 按用户查询已发布项目
CREATE INDEX IF NOT EXISTS `idx_project_user_status_created` ON `project` (`user_id`, `status`, `created_at` DESC);

-- V17: user_follow 关注列表
CREATE INDEX IF NOT EXISTS `idx_user_follow_following` ON `user_follow` (`following_id`, `created_at` DESC);

-- V17: user_follow 粉丝列表
CREATE INDEX IF NOT EXISTS `idx_user_follow_follower` ON `user_follow` (`follower_id`, `created_at` DESC);

-- V17: collab_session 按项目查询
CREATE INDEX IF NOT EXISTS `idx_collab_session_project` ON `collab_session` (`project_id`, `status`);

-- V17: collab_participant 按用户查询
CREATE INDEX IF NOT EXISTS `idx_collab_participant_user` ON `collab_participant` (`user_id`, `joined_at`);

-- V18: submission 用户做题历史
CREATE INDEX IF NOT EXISTS `idx_submission_user_problem_time` ON `submission` (`user_id`, `problem_id`, `created_at` DESC);

-- V18: project_like 按时间排序
CREATE INDEX IF NOT EXISTS `idx_project_like_project_time` ON `project_like` (`project_id`, `created_at` DESC);

-- V18: notification 按类型筛选未读
CREATE INDEX IF NOT EXISTS `idx_notification_user_type_read` ON `notification` (`user_id`, `type`, `is_read`);

-- V18: project 热门项目查询
CREATE INDEX IF NOT EXISTS `idx_project_hot` ON `project` (`status`, `like_count` DESC, `created_at` DESC);

-- V18: homework_submission 学生作业查询
CREATE INDEX IF NOT EXISTS `idx_homework_submission_student_status` ON `homework_submission` (`student_id`, `status`, `created_at` DESC);

-- V18: competition_ranking 竞赛排名查询
CREATE INDEX IF NOT EXISTS `idx_competition_ranking_rank` ON `competition_ranking` (`competition_id`, `rank`);

-- ============================================================
-- 初始数据
-- ============================================================

-- 默认管理员 (密码: admin123, BCrypt 加密)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2b$10$y3vcNmGVf0Y903xNnhr1IefOOJsmOdTr1QqzH7txQMBIPMDmMqa76', '管理员', 'ADMIN');

-- 默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('site.name', 'Scratch 社区', '站点名称'),
('site.description', '面向少儿编程的 Scratch 编程社区', '站点描述'),
('register.enabled', 'true', '是否开放注册'),
('upload.max_size', '52428800', '上传文件大小限制 (bytes)'),
('sensitive_words', '', '扩展敏感词库（逗号或换行分隔）');
