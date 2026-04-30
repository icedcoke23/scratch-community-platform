package com.scratch.community.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.module.user.entity.User;
import com.scratch.community.module.user.mapper.UserMapper;
import com.scratch.community.module.user.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师学情分析增强服务
 *
 * 提供班级维度的多维度数据分析:
 * - 学生活跃度分析
 * - 作业完成质量分析
 * - 项目创作趋势
 * - 学习进度追踪
 * - 预警学生识别
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherDashboardService {

    private final UserMapper userMapper;
    private final CrossModuleQueryRepository crossModuleQueryRepository;

    /**
     * 获取班级综合看板数据
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getClassDashboard(Long classId) {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        // 1. 班级基础统计
        dashboard.put("overview", getClassOverview(classId));

        // 2. 学生活跃度分析
        dashboard.put("activity", getActivityAnalysis(classId));

        // 3. 作业完成情况
        dashboard.put("homework", getHomeworkAnalysis(classId));

        // 4. 项目创作统计
        dashboard.put("projects", getProjectAnalysis(classId));

        // 5. 学习进度追踪
        dashboard.put("progress", getProgressTracking(classId));

        // 6. 预警学生
        dashboard.put("alerts", getAlertStudents(classId));

        return dashboard;
    }

    /**
     * 班级概览统计
     */
    private Map<String, Object> getClassOverview(Long classId) {
        Map<String, Object> overview = new LinkedHashMap<>();

        // 学生总数
        long totalStudents = crossModuleQueryRepository.countStudents();

        // 本周活跃学生（最近 7 天有登录或提交）
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long activeStudents = crossModuleQueryRepository.countStudentsActiveSince(weekAgo);

        // 作业总数
        long totalHomework = crossModuleQueryRepository.countHomeworkByClass(classId);

        // 项目总数
        long totalProjects = crossModuleQueryRepository.countAllProjects();

        overview.put("totalStudents", totalStudents);
        overview.put("activeStudents", activeStudents);
        overview.put("activityRate", totalStudents > 0 ?
                Math.round(activeStudents * 100.0 / totalStudents) : 0);
        overview.put("totalHomework", totalHomework);
        overview.put("totalProjects", totalProjects);

        return overview;
    }

    /**
     * 学生活跃度分析
     * 按周统计学生的活跃天数分布
     */
    private Map<String, Object> getActivityAnalysis(Long classId) {
        Map<String, Object> activity = new LinkedHashMap<>();

        // 最近 4 周的活跃趋势（按天统计）
        List<Map<String, Object>> weeklyTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int week = 3; week >= 0; week--) {
            LocalDate weekStart = today.minusWeeks(week).minusDays(today.getDayOfWeek().getValue() - 1);
            LocalDate weekEnd = weekStart.plusDays(6);

            Map<String, Object> weekData = new LinkedHashMap<>();
            weekData.put("weekStart", weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE));
            weekData.put("weekEnd", weekEnd.format(DateTimeFormatter.ISO_LOCAL_DATE));

            // 统计该周每天的活跃用户数
            List<Long> dailyActive = new ArrayList<>();
            for (int day = 0; day < 7; day++) {
                LocalDate date = weekStart.plusDays(day);
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

                long active = crossModuleQueryRepository.countStudentsActiveBetween(dayStart, dayEnd);
                dailyActive.add(active);
            }
            weekData.put("dailyActive", dailyActive);
            weeklyTrend.add(weekData);
        }

        activity.put("weeklyTrend", weeklyTrend);

        // 活跃度分布（高/中/低/不活跃）
        Map<String, Long> activityDistribution = new LinkedHashMap<>();
        activityDistribution.put("high", 0L);     // 每周活跃 >= 5 天
        activityDistribution.put("medium", 0L);   // 每周活跃 3-4 天
        activityDistribution.put("low", 0L);      // 每周活跃 1-2 天
        activityDistribution.put("inactive", 0L); // 本周无活跃

        // 简化统计：基于 updatedAt 判断
        long totalStudents = crossModuleQueryRepository.countStudents();
        long activeThisWeek = crossModuleQueryRepository.countStudentsActiveSince(LocalDateTime.now().minusDays(7));

        activityDistribution.put("active", activeThisWeek);
        activityDistribution.put("inactive", totalStudents - activeThisWeek);
        activity.put("distribution", activityDistribution);

        return activity;
    }

    /**
     * 作业完成情况分析
     */
    private Map<String, Object> getHomeworkAnalysis(Long classId) {
        Map<String, Object> homework = new LinkedHashMap<>();

        // 获取班级的所有作业
        List<Map<String, Object>> homeworkList = crossModuleQueryRepository.getHomeworkListByClass(classId, 10);

        // 每个作业的提交率
        List<Map<String, Object>> homeworkStats = new ArrayList<>();
        for (Map<String, Object> hw : homeworkList) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("id", hw.get("id"));
            stat.put("title", hw.get("title"));
            stat.put("deadline", hw.get("deadline") != null ? hw.get("deadline").toString() : null);

            long hwId = ((Number) hw.get("id")).longValue();
            long submissions = crossModuleQueryRepository.countSubmissionsByHomework(hwId);

            long totalStudents = crossModuleQueryRepository.countStudents();

            stat.put("submissions", submissions);
            stat.put("totalStudents", totalStudents);
            stat.put("submissionRate", totalStudents > 0 ?
                    Math.round(submissions * 100.0 / totalStudents) : 0);

            homeworkStats.add(stat);
        }

        homework.put("recentHomework", homeworkStats);

        // 平均提交率
        double avgRate = homeworkStats.stream()
                .mapToDouble(s -> (double) s.get("submissionRate"))
                .average()
                .orElse(0);
        homework.put("averageSubmissionRate", Math.round(avgRate));

        return homework;
    }

    /**
     * 项目创作统计
     */
    private Map<String, Object> getProjectAnalysis(Long classId) {
        Map<String, Object> projects = new LinkedHashMap<>();

        // 最近 30 天的创作趋势
        List<Map<String, Object>> dailyCreation = new ArrayList<>();
        for (int day = 29; day >= 0; day--) {
            LocalDate date = LocalDate.now().minusDays(day);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long count = crossModuleQueryRepository.countProjectsCreatedBetween(dayStart, dayEnd);

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
            dayData.put("count", count);
            dailyCreation.add(dayData);
        }
        projects.put("dailyCreation", dailyCreation);

        // 项目状态分布
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        statusDistribution.put("draft", crossModuleQueryRepository.countProjectsByStatus("draft"));
        statusDistribution.put("published", crossModuleQueryRepository.countProjectsByStatus("published"));
        projects.put("statusDistribution", statusDistribution);

        return projects;
    }

    /**
     * 学习进度追踪
     * 统计学生的等级和积分分布
     */
    private Map<String, Object> getProgressTracking(Long classId) {
        Map<String, Object> progress = new LinkedHashMap<>();

        // 等级分布
        List<Map<String, Object>> students = crossModuleQueryRepository.getStudentLevelAndPoints();

        Map<Integer, Long> levelDistribution = students.stream()
                .collect(Collectors.groupingBy(
                        m -> m.get("level") != null ? ((Number) m.get("level")).intValue() : 1,
                        Collectors.counting()));
        progress.put("levelDistribution", levelDistribution);

        // 积分区间分布
        Map<String, Long> pointsDistribution = new LinkedHashMap<>();
        pointsDistribution.put("0-100", students.stream()
                .filter(m -> m.get("points") != null && ((Number) m.get("points")).intValue() <= 100).count());
        pointsDistribution.put("101-500", students.stream()
                .filter(m -> m.get("points") != null && ((Number) m.get("points")).intValue() > 100
                        && ((Number) m.get("points")).intValue() <= 500).count());
        pointsDistribution.put("501-1000", students.stream()
                .filter(m -> m.get("points") != null && ((Number) m.get("points")).intValue() > 500
                        && ((Number) m.get("points")).intValue() <= 1000).count());
        pointsDistribution.put("1000+", students.stream()
                .filter(m -> m.get("points") != null && ((Number) m.get("points")).intValue() > 1000).count());
        progress.put("pointsDistribution", pointsDistribution);

        // 平均积分和等级
        progress.put("averagePoints", students.stream()
                .mapToInt(m -> m.get("points") != null ? ((Number) m.get("points")).intValue() : 0)
                .average().orElse(0));
        progress.put("averageLevel", students.stream()
                .mapToInt(m -> m.get("level") != null ? ((Number) m.get("level")).intValue() : 1)
                .average().orElse(0));

        return progress;
    }

    /**
     * 预警学生识别
     * 识别可能需要关注的学生
     */
    private List<Map<String, Object>> getAlertStudents(Long classId) {
        List<Map<String, Object>> alerts = new ArrayList<>();

        // 1. 长期不活跃学生（超过 14 天未登录）
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusDays(14);
        List<Map<String, Object>> inactiveStudents = crossModuleQueryRepository.getInactiveStudentsSince(twoWeeksAgo, 20);

        for (Map<String, Object> student : inactiveStudents) {
            Map<String, Object> alert = new LinkedHashMap<>();
            alert.put("userId", student.get("id"));
            alert.put("nickname", student.get("nickname"));
            alert.put("type", "INACTIVE");
            alert.put("message", "超过 14 天未活跃");
            alert.put("lastActive", student.get("updatedAt") != null ? student.get("updatedAt").toString() : "未知");
            alerts.add(alert);
        }

        // 2. 积分过低学生（低于班级平均的 50%）
        List<Map<String, Object>> allStudents = crossModuleQueryRepository.getStudentLevelAndPoints();

        double avgPoints = allStudents.stream()
                .mapToInt(m -> m.get("points") != null ? ((Number) m.get("points")).intValue() : 0)
                .average().orElse(0);

        double threshold = avgPoints * 0.5;
        for (Map<String, Object> student : allStudents) {
            int points = student.get("points") != null ? ((Number) student.get("points")).intValue() : 0;
            if (points < threshold && points >= 0) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("userId", student.get("id"));
                alert.put("nickname", student.get("nickname"));
                alert.put("type", "LOW_POINTS");
                alert.put("message", String.format("积分 %d 低于班级平均 50%% (%.0f)", points, threshold));
                alert.put("points", points);
                alerts.add(alert);
            }
        }

        return alerts;
    }
}
