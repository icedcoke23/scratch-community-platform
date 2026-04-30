package com.scratch.community.module.classroom.service;

import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.classroom.vo.ClassAnalyticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学情分析服务
 * 提供班级维度的教学数据统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取班级学情报告
     */
    @Transactional(readOnly = true)
    public ClassAnalyticsVO getClassAnalytics(Long classId, Long teacherId) {
        // 验证班级归属
        Integer classCheck = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM class WHERE id = ? AND teacher_id = ? AND deleted = 0",
                Integer.class, classId, teacherId);
        if (classCheck == null || classCheck == 0) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }

        ClassAnalyticsVO vo = new ClassAnalyticsVO();
        vo.setClassId(classId);

        // 班级基本信息
        jdbcTemplate.query(
                "SELECT name, student_count FROM class WHERE id = ? AND deleted = 0",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    vo.setClassName(rs.getString("name"));
                    vo.setStudentCount(rs.getInt("student_count"));
                },
                classId);

        // 作业统计
        List<ClassAnalyticsVO.HomeworkStat> homeworkStats = getHomeworkStats(classId, vo.getStudentCount());
        vo.setHomeworkStats(homeworkStats);
        vo.setHomeworkCount(homeworkStats.size());

        // 平均提交率和平均分
        if (!homeworkStats.isEmpty()) {
            vo.setAvgSubmitRate(homeworkStats.stream()
                    .mapToDouble(ClassAnalyticsVO.HomeworkStat::getSubmitRate)
                    .average().orElse(0));
            vo.setAvgScore(homeworkStats.stream()
                    .filter(s -> s.getAvgScore() > 0)
                    .mapToDouble(ClassAnalyticsVO.HomeworkStat::getAvgScore)
                    .average().orElse(0));
        }

        // 最近 7 天活跃学生数（有提交记录的）
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT hs.student_id) FROM homework_submission hs " +
                        "JOIN homework h ON hs.homework_id = h.id " +
                        "WHERE h.class_id = ? AND hs.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) AND hs.deleted = 0",
                Integer.class, classId);
        vo.setActiveStudents7d(activeCount != null ? activeCount : 0);

        // 学生排名
        vo.setStudentRanks(getStudentRanks(classId, homeworkStats.size()));

        // 题目类型通过率
        vo.setTypePassRates(getTypePassRates(classId));

        return vo;
    }

    /**
     * 获取各作业统计（优化：单次聚合查询，消除 N+1）
     */
    private List<ClassAnalyticsVO.HomeworkStat> getHomeworkStats(Long classId, int studentCount) {
        // 单次查询：作业基本信息 + 每个作业的平均分（LEFT JOIN 聚合）
        return jdbcTemplate.query(
                "SELECT h.id, h.title, h.submit_count, h.graded_count, h.total_score, " +
                        "COALESCE(AVG(hs.score), 0) AS avg_score " +
                        "FROM homework h " +
                        "LEFT JOIN homework_submission hs ON h.id = hs.homework_id AND hs.status = 'graded' AND hs.deleted = 0 " +
                        "WHERE h.class_id = ? AND h.deleted = 0 " +
                        "GROUP BY h.id, h.title, h.submit_count, h.graded_count, h.total_score " +
                        "ORDER BY h.created_at DESC",
                (rs, rowNum) -> {
                    ClassAnalyticsVO.HomeworkStat stat = new ClassAnalyticsVO.HomeworkStat();
                    stat.setHomeworkId(rs.getLong("id"));
                    stat.setTitle(rs.getString("title"));
                    stat.setSubmitCount(rs.getInt("submit_count"));
                    stat.setTotalStudents(studentCount);
                    stat.setSubmitRate(studentCount > 0
                            ? (double) rs.getInt("submit_count") / studentCount * 100 : 0);
                    stat.setGradedCount(rs.getInt("graded_count"));
                    stat.setAvgScore(rs.getInt("avg_score"));
                    return stat;
                },
                classId);
    }

    /**
     * 获取学生排名
     */
    private List<ClassAnalyticsVO.StudentRank> getStudentRanks(Long classId, int totalHomework) {
        // 查询班级学生及其作业提交情况
        List<ClassAnalyticsVO.StudentRank> ranks = jdbcTemplate.query(
                "SELECT u.id, u.username, u.nickname, u.avatar_url, " +
                        "COALESCE(sub.submitted_count, 0) AS submitted_count, " +
                        "COALESCE(sub.avg_score, 0) AS avg_score, " +
                        "COALESCE(pt.total_points, 0) AS total_points, " +
                        "COALESCE(pt.level, 1) AS level " +
                        "FROM class_student cs " +
                        "JOIN user u ON cs.student_id = u.id AND u.deleted = 0 " +
                        "LEFT JOIN (" +
                        "  SELECT hs.student_id, COUNT(*) AS submitted_count, AVG(hs.score) AS avg_score " +
                        "  FROM homework_submission hs " +
                        "  JOIN homework h ON hs.homework_id = h.id " +
                        "  WHERE h.class_id = ? AND hs.deleted = 0 " +
                        "  GROUP BY hs.student_id" +
                        ") sub ON cs.student_id = sub.student_id " +
                        "LEFT JOIN (" +
                        "  SELECT user_id, SUM(points) AS total_points, " +
                        "    CASE " +
                        "      WHEN SUM(points) >= 12000 THEN 8 " +
                        "      WHEN SUM(points) >= 6000 THEN 7 " +
                        "      WHEN SUM(points) >= 3000 THEN 6 " +
                        "      WHEN SUM(points) >= 1500 THEN 5 " +
                        "      WHEN SUM(points) >= 700 THEN 4 " +
                        "      WHEN SUM(points) >= 300 THEN 3 " +
                        "      WHEN SUM(points) >= 100 THEN 2 " +
                        "      ELSE 1 " +
                        "    END AS level " +
                        "  FROM point_log GROUP BY user_id" +
                        ") pt ON cs.student_id = pt.user_id " +
                        "WHERE cs.class_id = ? " +
                        "ORDER BY avg_score DESC, submitted_count DESC",
                (rs, rowNum) -> {
                    ClassAnalyticsVO.StudentRank rank = new ClassAnalyticsVO.StudentRank();
                    rank.setStudentId(rs.getLong("id"));
                    rank.setUsername(rs.getString("username"));
                    rank.setNickname(rs.getString("nickname"));
                    rank.setAvatarUrl(rs.getString("avatar_url"));
                    rank.setSubmittedCount(rs.getInt("submitted_count"));
                    rank.setTotalHomework(totalHomework);
                    rank.setSubmitRate(totalHomework > 0
                            ? (double) rs.getInt("submitted_count") / totalHomework * 100 : 0);
                    rank.setAvgScore(rs.getDouble("avg_score"));
                    rank.setTotalPoints(rs.getInt("total_points"));
                    rank.setLevel(rs.getInt("level"));
                    return rank;
                },
                classId, classId);
        return ranks;
    }

    /**
     * 获取题目类型通过率
     */
    private Map<String, Double> getTypePassRates(Long classId) {
        Map<String, Double> rates = new LinkedHashMap<>();

        // 查询该班级学生在各题型上的通过率
        jdbcTemplate.query(
                "SELECT p.type, " +
                        "COUNT(*) AS total, " +
                        "SUM(CASE WHEN s.verdict = 'AC' THEN 1 ELSE 0 END) AS ac_count " +
                        "FROM submission s " +
                        "JOIN problem p ON s.problem_id = p.id " +
                        "JOIN class_student cs ON s.user_id = cs.student_id " +
                        "WHERE cs.class_id = ? AND s.deleted = 0 AND p.deleted = 0 " +
                        "GROUP BY p.type",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    String type = rs.getString("type");
                    int total = rs.getInt("total");
                    int acCount = rs.getInt("ac_count");
                    double rate = total > 0 ? (double) acCount / total * 100 : 0;
                    String typeName = switch (type) {
                        case "choice" -> "选择题";
                        case "true_false" -> "判断题";
                        case "scratch_algo" -> "编程题";
                        default -> type;
                    };
                    rates.put(typeName, Math.round(rate * 100.0) / 100.0);
                },
                classId);

        return rates;
    }

    /**
     * 获取学生个人学情详情
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStudentAnalytics(Long studentId, Long classId) {
        Map<String, Object> result = new HashMap<>();

        // 基本信息
        jdbcTemplate.query(
                "SELECT u.username, u.nickname, u.avatar_url FROM user u WHERE u.id = ? AND u.deleted = 0",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    result.put("username", rs.getString("username"));
                    result.put("nickname", rs.getString("nickname"));
                    result.put("avatarUrl", rs.getString("avatar_url"));
                },
                studentId);

        // 作业完成情况
        List<Map<String, Object>> homeworkDetails = jdbcTemplate.query(
                "SELECT h.id, h.title, h.total_score, h.deadline, " +
                        "hs.status, hs.score, hs.comment, hs.created_at AS submitted_at, hs.graded_at " +
                        "FROM homework h " +
                        "LEFT JOIN homework_submission hs ON h.id = hs.homework_id AND hs.student_id = ? AND hs.deleted = 0 " +
                        "WHERE h.class_id = ? AND h.deleted = 0 AND h.status != 'draft' " +
                        "ORDER BY h.created_at DESC",
                (rs, rowNum) -> {
                    Map<String, Object> hw = new HashMap<>();
                    hw.put("homeworkId", rs.getLong("id"));
                    hw.put("title", rs.getString("title"));
                    hw.put("totalScore", rs.getInt("total_score"));
                    hw.put("deadline", rs.getTimestamp("deadline"));
                    hw.put("status", rs.getString("status")); // null = 未提交
                    hw.put("score", rs.getObject("score"));
                    hw.put("comment", rs.getString("comment"));
                    hw.put("submittedAt", rs.getTimestamp("submitted_at"));
                    hw.put("gradedAt", rs.getTimestamp("graded_at"));
                    return hw;
                },
                studentId, classId);
        result.put("homeworkDetails", homeworkDetails);

        // 判题统计
        jdbcTemplate.query(
                "SELECT " +
                        "COUNT(*) AS total_submissions, " +
                        "SUM(CASE WHEN verdict = 'AC' THEN 1 ELSE 0 END) AS ac_count, " +
                        "SUM(CASE WHEN verdict = 'WA' THEN 1 ELSE 0 END) AS wa_count, " +
                        "SUM(CASE WHEN verdict = 'TLE' THEN 1 ELSE 0 END) AS tle_count " +
                        "FROM submission WHERE user_id = ? AND deleted = 0",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    result.put("totalSubmissions", rs.getInt("total_submissions"));
                    result.put("acCount", rs.getInt("ac_count"));
                    result.put("waCount", rs.getInt("wa_count"));
                    result.put("tleCount", rs.getInt("tle_count"));
                },
                studentId);

        // 积分和等级
        Integer points = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(points), 0) FROM point_log WHERE user_id = ?",
                Integer.class, studentId);
        result.put("totalPoints", points != null ? points : 0);

        return result;
    }
}
