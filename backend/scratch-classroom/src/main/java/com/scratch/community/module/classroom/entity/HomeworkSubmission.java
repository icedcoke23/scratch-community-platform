package com.scratch.community.module.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 作业提交实体
 * 对应 database homework_submission 表
 *
 * 注意：answers 是 JSON 字符串，直接存储不经过 JacksonTypeHandler
 */
@Getter
@Setter
@TableName(value = "homework_submission", autoResultMap = true)
public class HomeworkSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业 ID */
    private Long homeworkId;

    /** 学生 ID */
    private Long studentId;

    /** 提交的项目 ID（Scratch 项目作业） */
    private Long projectId;

    /** 提交的答案 JSON 字符串（选择题作业） */
    private String answers;

    /** 得分 */
    private Integer score;

    /** 教师评语 */
    private String comment;

    /**
     * 状态:
     * - submitted: 已提交
     * - graded: 已批改
     * - returned: 已退回（需重新提交）
     */
    private String status;

    /** 提交时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 批改时间 */
    private LocalDateTime gradedAt;

    @TableLogic
    private Integer deleted;
}
