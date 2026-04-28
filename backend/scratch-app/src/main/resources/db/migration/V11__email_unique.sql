-- V11: 添加 email 唯一约束
-- 先清理可能的重复 email（保留 id 最大的记录，即最新的）
DELETE u1 FROM `user` u1
INNER JOIN `user` u2
    ON u1.email = u2.email
    AND u1.email IS NOT NULL
    AND u1.id < u2.id;

-- 添加 email 唯一约束（允许 NULL，MySQL 中多个 NULL 不违反 UNIQUE）
ALTER TABLE `user` ADD UNIQUE KEY `uk_email` (`email`);
