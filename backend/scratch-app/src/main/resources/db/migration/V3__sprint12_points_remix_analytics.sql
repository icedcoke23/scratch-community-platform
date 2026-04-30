-- ============================================================
-- V3: Sprint 12 — 积分体系 + Remix + 学情分析
-- ============================================================

-- 积分变动记录表
CREATE TABLE IF NOT EXISTS `point_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `type` VARCHAR(30) NOT NULL COMMENT '变动类型: DAILY_CHECKIN/PUBLISH_PROJECT/RECEIVE_LIKE/AC_SUBMISSION/COMPLETE_HOMEWORK/ADMIN_ADJUST',
    `points` INT NOT NULL COMMENT '变动积分（正数增加，负数扣减）',
    `total_points` INT NOT NULL COMMENT '变动后总积分',
    `ref_type` VARCHAR(30) DEFAULT NULL COMMENT '关联对象类型',
    `ref_id` BIGINT DEFAULT NULL COMMENT '关联对象 ID',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_user_date` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动记录表';

-- 用户表新增积分和等级字段
ALTER TABLE `user`
    ADD COLUMN `points` INT NOT NULL DEFAULT 0 COMMENT '总积分' AFTER `status`,
    ADD COLUMN `level` INT NOT NULL DEFAULT 1 COMMENT '等级' AFTER `points`;

-- 项目表新增 Remix 字段
ALTER TABLE `project`
    ADD COLUMN `remix_project_id` BIGINT DEFAULT NULL COMMENT '原始项目 ID（Remix 来源）' AFTER `tags`,
    ADD COLUMN `remix_count` INT NOT NULL DEFAULT 0 COMMENT 'Remix 次数' AFTER `remix_project_id`;

-- 创建积分排行榜索引
CREATE INDEX idx_user_points ON `user` (`points` DESC);

-- 初始积分：为现有用户从判题记录中计算积分
UPDATE `user` u SET
    `points` = (
        SELECT COALESCE(
            (SELECT COUNT(*) FROM submission WHERE user_id = u.id AND verdict = 'AC') * 15
            + (SELECT COUNT(DISTINCT DATE(created_at)) FROM point_log WHERE user_id = u.id AND type = 'DAILY_CHECKIN') * 5,
            0
        )
    ),
    `level` = CASE
        WHEN (SELECT COALESCE(SUM(points), 0) FROM point_log WHERE user_id = u.id) >= 12000 THEN 8
        WHEN (SELECT COALESCE(SUM(points), 0) FROM point_log WHERE user_id = u.id) >= 6000 THEN 7
        WHEN (SELECT COALESCE(SUM(points), 0) FROM point_log WHERE user_id = u.id) >= 3000 THEN 6
        WHEN (SELECT COALESCE(SUM(points), 0) FROM point_log WHERE user_id = u.id) >= 1500 THEN 5
        WHEN (SELECT COALESCE(SUM(points), 0) FROM point_log WHERE user_id = u.id) >= 700 THEN 4
        WHEN (SELECT COALESCE(SUM(points), 0) FROM point_log WHERE user_id = u.id) >= 300 THEN 3
        WHEN (SELECT COALESCE(SUM(points), 0) FROM point_log WHERE user_id = u.id) >= 100 THEN 2
        ELSE 1
    END
WHERE `deleted` = 0;
