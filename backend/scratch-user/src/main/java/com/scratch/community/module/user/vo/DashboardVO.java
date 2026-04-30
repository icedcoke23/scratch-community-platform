package com.scratch.community.module.user.vo;

import lombok.Data;

/**
 * 管理后台数据统计 VO
 */
@Data
public class DashboardVO {

    /** 总用户数 */
    private Long totalUsers;

    /** 今日新增用户 */
    private Long todayNewUsers;

    /** 总项目数 */
    private Long totalProjects;

    /** 已发布项目数 */
    private Long publishedProjects;

    /** 总题目数 */
    private Long totalProblems;

    /** 总提交数 */
    private Long totalSubmissions;

    /** AC 提交数 */
    private Long acSubmissions;

    /** 总班级数 */
    private Long totalClasses;

    /** 总作业数 */
    private Long totalHomework;

    /** 待审核记录数 */
    private Long pendingAudits;
}
