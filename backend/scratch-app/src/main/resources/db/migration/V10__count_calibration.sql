-- V10: 冗余计数字段校准标记
-- 实际校准逻辑由 CountCalibrationScheduler 每天凌晨 3 点执行
-- 本迁移脚本仅作为 Flyway 版本标记，无 DDL 变更
SELECT 1;
