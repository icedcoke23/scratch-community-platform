-- V21__point_idempotency_and_notification_archive.sql
-- 优化项：积分事件幂等性、通知表归档、补充索引、事件死信队列

-- ==================== 1. 积分事件幂等性改造 ====================
-- 为 point_log 表添加唯一约束，防止重复积分
-- 约束规则：同一用户、同一类型、同一引用ID只能有一条记录
ALTER TABLE point_log 
ADD CONSTRAINT uk_point_log_user_type_ref 
UNIQUE KEY (user_id, type, ref_type, ref_id);

-- ==================== 2. 通知表归档 ====================
-- 创建通知归档表
CREATE TABLE IF NOT EXISTS notification_archive (
    id BIGINT PRIMARY KEY COMMENT '原通知 ID',
    user_id BIGINT NOT NULL COMMENT '接收用户 ID',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    type VARCHAR(50) NOT NULL COMMENT '通知类型：SYSTEM/COMMENT/LIKE/SYSTEM_ANNOUNCEMENT',
    ref_type VARCHAR(50) COMMENT '关联类型：project/comment/homework',
    ref_id BIGINT COMMENT '关联 ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    read_at DATETIME COMMENT '阅读时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    archived_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    INDEX idx_archive_user_created (user_id, created_at),
    INDEX idx_archive_user_read (user_id, is_read, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='通知归档表';

-- ==================== 3. 补充数据库索引 ====================
-- point_log 关键查询索引
CREATE INDEX IF NOT EXISTS idx_point_log_user_type_date 
ON point_log(user_id, type, created_at);

CREATE INDEX IF NOT EXISTS idx_point_log_user_total 
ON point_log(user_id, total_points DESC);

-- notification 表索引优化
CREATE INDEX IF NOT EXISTS idx_notification_user_unread 
ON notification(user_id, is_read, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_type_date 
ON notification(type, created_at DESC);

-- project_comment 查询优化
CREATE INDEX IF NOT EXISTS idx_project_comment_project_created 
ON project_comment(project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_project_comment_user_created 
ON project_comment(user_id, created_at DESC);

-- homework_submission 查询优化
CREATE INDEX IF NOT EXISTS idx_homework_submission_homework_status 
ON homework_submission(homework_id, status, deleted);

CREATE INDEX IF NOT EXISTS idx_homework_submission_user_status 
ON homework_submission(user_id, status, deleted);

-- project_like 批量查询优化
CREATE INDEX IF NOT EXISTS idx_project_like_user_project 
ON project_like(user_id, project_id);

-- ==================== 4. 事件死信队列 ====================
-- 创建事件失败日志表，用于追踪和重试失败的事件
CREATE TABLE IF NOT EXISTS event_dead_letter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL COMMENT '事件类型：PointEvent/NotificationEvent等',
    event_data JSON NOT NULL COMMENT '事件完整数据',
    failure_reason TEXT COMMENT '失败原因',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retries INT DEFAULT 3 COMMENT '最大重试次数',
    next_retry_at DATETIME COMMENT '下次重试时间',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/RETRYING/SUCCESS/FAILED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at DATETIME COMMENT '解决时间',
    resolved_by VARCHAR(100) COMMENT '解决人',
    resolution_note TEXT COMMENT '解决说明',
    INDEX idx_event_status_retry (status, next_retry_at),
    INDEX idx_event_type_created (event_type, created_at),
    INDEX idx_event_pending (status, next_retry_at, retry_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='事件死信队列表';

-- ==================== 5. 轮播图默认数据（如果不存在） ====================
-- 检查并插入默认轮播图数据
INSERT INTO carousel (title, image_url, target_url, sort_order, is_active, created_at)
SELECT '欢迎使用 Scratch 社区', '/images/carousel/welcome.jpg', '/editor', 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM carousel LIMIT 1);

INSERT INTO carousel (title, image_url, target_url, sort_order, is_active, created_at)
SELECT '参加编程竞赛', '/images/carousel/competition.jpg', '/competition', 2, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM carousel WHERE sort_order = 2 LIMIT 1);

INSERT INTO carousel (title, image_url, target_url, sort_order, is_active, created_at)
SELECT '学习编程教程', '/images/carousel/tutorial.jpg', '/problems', 3, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM carousel WHERE sort_order = 3 LIMIT 1);
