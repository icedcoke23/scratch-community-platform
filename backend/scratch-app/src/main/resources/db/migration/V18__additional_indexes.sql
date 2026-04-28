-- V18: 补充高频查询缺失索引
-- 基于深度分析报告的索引优化建议

-- 1. submission: 用户做题历史查询 (user_id + problem_id + created_at DESC)
CREATE INDEX `idx_submission_user_problem_time`
    ON `submission` (`user_id`, `problem_id`, `created_at` DESC);

-- 2. project_like: 按时间排序的点赞列表 (project_id + created_at DESC)
CREATE INDEX `idx_project_like_project_time`
    ON `project_like` (`project_id`, `created_at` DESC);

-- 3. notification: 按类型筛选未读通知 (user_id + type + is_read)
CREATE INDEX `idx_notification_user_type_read`
    ON `notification` (`user_id`, `type`, `is_read`);

-- 4. project: 热门项目查询优化 (status + like_count DESC + created_at DESC)
CREATE INDEX `idx_project_hot`
    ON `project` (`status`, `like_count` DESC, `created_at` DESC);

-- 5. homework_submission: 学生作业查询 (student_id + status + created_at DESC)
CREATE INDEX `idx_homework_submission_student_status`
    ON `homework_submission` (`student_id`, `status`, `created_at` DESC);

-- 6. user_follow: 关注列表查询 (following_id + created_at DESC)
CREATE INDEX `idx_user_follow_following_time`
    ON `user_follow` (`following_id`, `created_at` DESC);

-- 7. user_follow: 粉丝列表查询 (follower_id + created_at DESC)
CREATE INDEX `idx_user_follow_follower_time`
    ON `user_follow` (`follower_id`, `created_at` DESC);

-- 8. competition_ranking: 竞赛排名查询 (competition_id + rank)
CREATE INDEX `idx_competition_ranking_rank`
    ON `competition_ranking` (`competition_id`, `rank`);
