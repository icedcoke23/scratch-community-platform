-- V14: 为关键 Entity 添加乐观锁 version 字段
-- 防止高并发场景下 like_count/comment_count/view_count/points/level 等字段的数据不一致

-- 项目表：保护 like_count, comment_count, view_count 的并发更新
ALTER TABLE `project`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `deleted`;

-- 用户表：保护 points, level 的并发更新
ALTER TABLE `user`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `deleted`;

-- 竞赛排名表：保护 total_score, rank 的并发更新
ALTER TABLE `competition_ranking`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `updated_at`;

-- 作业表：保护 submit_count, graded_count 的并发更新
ALTER TABLE `homework`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `deleted`;
