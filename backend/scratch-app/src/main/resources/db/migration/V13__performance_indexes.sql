-- V13: 补充性能索引与数据完整性优化

-- 1. homework_problem 索引
CREATE INDEX `idx_homework_problem_homework` ON `homework_problem` (`homework_id`, `sort_order`);

-- 2. competition_problem 索引
CREATE INDEX `idx_competition_problem_competition` ON `competition_problem` (`competition_id`, `sort_order`);

-- 3. submission 竞赛查询索引
CREATE INDEX `idx_submission_competition_user` ON `submission` (`competition_id`, `user_id`, `verdict`);

-- 4. point_log 每日签到检查优化
CREATE INDEX `idx_point_log_checkin` ON `point_log` (`user_id`, `type`, `created_at`);

-- 5. ai_review 按项目查询最新
CREATE INDEX `idx_ai_review_project_latest` ON `ai_review` (`project_id`, `created_at` DESC);

-- 6. competition 按状态+时间查询
CREATE INDEX `idx_competition_status_time` ON `competition` (`status`, `start_time`, `end_time`);
