package com.scratch.community.module.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.module.user.entity.PointLog;
import com.scratch.community.module.user.entity.User;
import com.scratch.community.module.user.mapper.PointLogMapper;
import com.scratch.community.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 积分服务
 *
 * 积分规则:
 * - 每日签到: +5
 * - 发布项目: +10
 * - 收到点赞: +2（每个点赞，每日上限 50）
 * - 判题通过 (AC): +15
 * - 完成作业: +20
 * - 管理员调整: 自定义
 *
 * 并发安全:
 * - 使用 Redisson 分布式锁保证积分累加的原子性
 * - Redisson 不可用时降级为数据库原子 SQL（已足够安全）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointLogMapper pointLogMapper;
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final CrossModuleQueryRepository crossModuleQuery;

    /** 可选依赖：Redisson 不可用时降级为数据库原子 SQL */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RedissonClient redissonClient;

    /** 积分等级阈值 */
    private static final int[] LEVEL_THRESHOLDS = {0, 100, 300, 700, 1500, 3000, 6000, 12000};

    /** 积分规则常量 */
    public static final int POINTS_DAILY_CHECKIN = 5;
    public static final int POINTS_PUBLISH_PROJECT = 10;
    public static final int POINTS_RECEIVE_LIKE = 2;
    public static final int POINTS_AC_SUBMISSION = 15;
    public static final int POINTS_COMPLETE_HOMEWORK = 20;

    /** 每日点赞积分上限 */
    private static final int DAILY_LIKE_POINTS_CAP = 50;

    /** 锁前缀 */
    private static final String LOCK_PREFIX = "lock:point:";

    // ==================== 积分操作 ====================

    /**
     * 每日签到
     * @return 本次获得积分，已签到返回 0
     */
    @Transactional
    public int dailyCheckin(Long userId) {
        if (crossModuleQuery.hasCheckedInToday(userId)) {
            return 0;
        }
        return addPoints(userId, POINTS_DAILY_CHECKIN, "DAILY_CHECKIN", "user", userId, "每日签到");
    }

    /**
     * 发布项目奖励
     */
    @Transactional
    public void onProjectPublished(Long userId, Long projectId) {
        addPoints(userId, POINTS_PUBLISH_PROJECT, "PUBLISH_PROJECT", "project", projectId, "发布项目");
    }

    /**
     * 收到点赞奖励
     * @return 实际获得积分（可能为 0，如果达到每日上限）
     */
    @Transactional
    public int onReceiveLike(Long userId, Long projectId) {
        // 检查今日点赞积分是否已达上限
        int todayLikePoints = crossModuleQuery.getTodayPointsByType(userId, "RECEIVE_LIKE");
        if (todayLikePoints >= DAILY_LIKE_POINTS_CAP) {
            return 0;
        }
        return addPoints(userId, POINTS_RECEIVE_LIKE, "RECEIVE_LIKE", "project", projectId, "收到点赞");
    }

    /**
     * 判题通过奖励
     */
    @Transactional
    public void onACSubmission(Long userId, Long submissionId) {
        if (pointLogMapper.countByTypeAndRef(userId, "AC_SUBMISSION", "submission", submissionId) > 0) {
            return;
        }
        addPoints(userId, POINTS_AC_SUBMISSION, "AC_SUBMISSION", "submission", submissionId, "判题通过");
    }

    /**
     * 完成作业奖励
     */
    @Transactional
    public void onHomeworkComplete(Long userId, Long homeworkId) {
        addPoints(userId, POINTS_COMPLETE_HOMEWORK, "COMPLETE_HOMEWORK", "homework", homeworkId, "完成作业");
    }

    /**
     * 管理员调整积分
     */
    @Transactional
    public void adminAdjust(Long userId, int points, String remark) {
        addPoints(userId, points, "ADMIN_ADJUST", "user", userId, remark);
    }

    // ==================== 查询 ====================

    /**
     * 获取用户积分信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserPoints(Long userId) {
        User user = userMapper.selectById(userId);
        int points = user != null ? getPointsFromUser(user) : 0;
        int level = calculateLevel(points);
        int nextLevelPoints = level < LEVEL_THRESHOLDS.length ? LEVEL_THRESHOLDS[level] : LEVEL_THRESHOLDS[LEVEL_THRESHOLDS.length - 1];
        int currentLevelPoints = LEVEL_THRESHOLDS[level - 1];

        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        result.put("level", level);
        result.put("levelName", getLevelName(level));
        result.put("nextLevelPoints", nextLevelPoints);
        result.put("currentLevelPoints", currentLevelPoints);
        result.put("progress", nextLevelPoints > currentLevelPoints
                ? (double)(points - currentLevelPoints) / (nextLevelPoints - currentLevelPoints) * 100
                : 100.0);
        return result;
    }

    /**
     * 获取积分变动记录（分页）
     */
    @Transactional(readOnly = true)
    public Page<PointLog> getPointLogs(Long userId, Page<PointLog> page) {
        return pointLogMapper.selectPage(page,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId)
                        .orderByDesc(PointLog::getCreatedAt));
    }

    /**
     * 获取积分排行榜
     *
     * <p>优化: 使用 JOIN 替代关联子查询，避免每行执行一次 SUM 查询。
     * 原查询: (SELECT COALESCE(SUM(points), 0) FROM point_log WHERE user_id = u.id) — 关联子查询
     * 优化后: LEFT JOIN + GROUP BY — 单次聚合
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPointRanking(int topN) {
        List<Map<String, Object>> ranking = crossModuleQuery.getPointRanking(topN);
        // 补充等级信息
        for (Map<String, Object> row : ranking) {
            int points = (int) row.get("points");
            row.put("level", calculateLevel(points));
        }
        return ranking;
    }

    // ==================== 等级计算 ====================

    public int calculateLevel(int points) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (points >= LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    public String getLevelName(int level) {
        return switch (level) {
            case 1 -> "编程新手";
            case 2 -> "编程学徒";
            case 3 -> "编程达人";
            case 4 -> "编程高手";
            case 5 -> "编程专家";
            case 6 -> "编程大师";
            case 7 -> "编程宗师";
            case 8 -> "编程传奇";
            default -> "未知等级";
        };
    }

    // ==================== 私有方法 ====================

    /**
     * 增加积分（核心方法）
     *
     * 并发安全策略:
     * 1. 优先使用 Redisson 分布式锁（多实例部署时安全）
     * 2. Redisson 不可用时降级为数据库原子 SQL UPDATE（单实例足够安全）
     */
    private int addPoints(Long userId, int points, String type, String refType, Long refId, String remark) {
        String lockKey = LOCK_PREFIX + userId;

        if (redissonClient != null) {
            // 使用分布式锁
            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                    try {
                        return doAddPoints(userId, points, type, refType, refId, remark);
                    } finally {
                        lock.unlock();
                    }
                } else {
                    log.warn("获取积分锁失败，降级为无锁模式: userId={}, type={}", userId, type);
                    return doAddPoints(userId, points, type, refType, refId, remark);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("积分锁被中断: userId={}, type={}", userId, type);
                return doAddPoints(userId, points, type, refType, refId, remark);
            }
        } else {
            // Redisson 不可用，降级为数据库原子 SQL
            return doAddPoints(userId, points, type, refType, refId, remark);
        }
    }

    /**
     * 实际执行积分累加（数据库原子操作）
     *
     * 使用单条 UPDATE 同时更新积分和等级，减少竞态窗口。
     * totalPoints 从 UPDATE 后的 SELECT 获取，在有锁保护下是安全的。
     * 无锁降级模式下，totalPoints 可能有微小偏差（可接受）。
     */
    private int doAddPoints(Long userId, int points, String type, String refType, Long refId, String remark) {
        // 原子更新积分（GREATEST 防止负数）
        jdbcTemplate.update(
                "UPDATE user SET points = GREATEST(points + ?, 0) WHERE id = ?",
                points, userId);

        // 获取更新后的积分和等级（单次查询，减少竞态窗口）
        Integer newTotal = jdbcTemplate.queryForObject(
                "SELECT COALESCE(points, 0) FROM user WHERE id = ?", Integer.class, userId);
        int total = newTotal != null ? newTotal : 0;

        // 更新等级（积分已确定，等级计算无并发问题）
        int newLevel = calculateLevel(total);
        jdbcTemplate.update("UPDATE user SET level = ? WHERE id = ? AND level != ?", newLevel, userId, newLevel);

        // 记录积分变动
        PointLog pointLog = new PointLog();
        pointLog.setUserId(userId);
        pointLog.setType(type);
        pointLog.setPoints(points);
        pointLog.setTotalPoints(total);
        pointLog.setRefType(refType);
        pointLog.setRefId(refId);
        pointLog.setRemark(remark);
        pointLogMapper.insert(pointLog);

        log.info("积分变动: userId={}, type={}, points={}, total={}", userId, type, points, total);
        return points;
    }

    private int getPointsFromUser(User user) {
        return crossModuleQuery.getUserPoints(user.getId());
    }
}
