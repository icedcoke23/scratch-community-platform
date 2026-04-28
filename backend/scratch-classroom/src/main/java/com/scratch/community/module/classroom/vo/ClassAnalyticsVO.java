package com.scratch.community.module.classroom.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 班级学情报告 VO
 */
@Data
public class ClassAnalyticsVO {

    /** 班级 ID */
    private Long classId;

    /** 班级名称 */
    private String className;

    /** 学生人数 */
    private int studentCount;

    /** 作业总数 */
    private int homeworkCount;

    /** 作业平均提交率 (%) */
    private double avgSubmitRate;

    /** 作业平均分 */
    private double avgScore;

    /** 最近 7 天活跃学生数 */
    private int activeStudents7d;

    /** 各作业统计 */
    private List<HomeworkStat> homeworkStats;

    /** 学生排名（按平均分） */
    private List<StudentRank> studentRanks;

    /** 题目类型通过率 */
    private Map<String, Double> typePassRates;

    @Data
    public static class HomeworkStat {
        private Long homeworkId;
        private String title;
        private int submitCount;
        private int totalStudents;
        private double submitRate; // 提交率 %
        private double avgScore;
        private int gradedCount;
    }

    @Data
    public static class StudentRank {
        private Long studentId;
        private String username;
        private String nickname;
        private String avatarUrl;
        private int submittedCount; // 已提交作业数
        private int totalHomework;  // 总作业数
        private double submitRate;  // 提交率 %
        private double avgScore;    // 平均分
        private int totalPoints;    // 总积分
        private int level;          // 等级
    }
}
