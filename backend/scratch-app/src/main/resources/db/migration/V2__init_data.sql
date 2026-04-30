-- ============================================================
-- V2: 初始数据
-- ============================================================

-- 默认管理员 (密码: admin123, BCrypt 加密)
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2b$10$y3vcNmGVf0Y903xNnhr1IefOOJsmOdTr1QqzH7txQMBIPMDmMqa76', '管理员', 'ADMIN');

-- 默认系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('site.name', 'Scratch 社区', '站点名称'),
('site.description', '面向少儿编程的 Scratch 编程社区', '站点描述'),
('register.enabled', 'true', '是否开放注册'),
('upload.max_size', '52428800', '上传文件大小限制 (bytes)');
