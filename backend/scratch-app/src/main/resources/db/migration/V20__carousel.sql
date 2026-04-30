-- 轮播图表
CREATE TABLE IF NOT EXISTS `carousel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(100) NOT NULL COMMENT '轮播图标题',
    `image_url` VARCHAR(500) NOT NULL COMMENT '图片 URL',
    `target_url` VARCHAR(500) DEFAULT NULL COMMENT '点击跳转链接',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述信息',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

-- 初始化默认轮播图数据
INSERT INTO `carousel` (`title`, `image_url`, `target_url`, `description`, `sort_order`, `is_enabled`) VALUES
('欢迎使用 Scratch 社区', 'https://picsum.photos/seed/scratch1/1200/400', '/feed', '开启你的编程之旅', 1, 1),
('创意作品展示', 'https://picsum.photos/seed/scratch2/1200/400', '/rank', '发现更多精彩作品', 2, 1),
('参加编程竞赛', 'https://picsum.photos/seed/scratch3/1200/400', '/competition', '挑战自我，赢得荣誉', 3, 1);
