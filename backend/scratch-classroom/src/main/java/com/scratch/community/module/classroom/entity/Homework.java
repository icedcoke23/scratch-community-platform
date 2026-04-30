package com.scratch.community.module.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 作业实体
 * 对应 database homework 表
 *
 * 注意：problemIds 是 JSON 字符串，直接存储不经过 JacksonTypeHandler
 */
@Getter
@Setter
@TableName(value = "homework", autoResultMap = true)
public class Homework {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 班级 ID */
    private Long classId;

    /** 创建者 ID（教师） */
    private Long teacherId;

    /** 作业标题 */
    private String title;

    /** 作业描述（Markdown） */
    private String description;

    /**
     * 作业类型:
     * - scratch_project: Scratch 项目创作
     * - choice: 选择题
     * - mixed: 混合（项目+选择题）
     */
    private String type;

    /** 关联的题目 ID 列表（JSON 字符串，选择题时使用） */
    private String problemIds;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 满分 */
    private Integer totalScore;

    /** 状态: draft / published / closed */
    private String status;

    /** 提交人数 */
    private Integer submitCount;

    /** 已批改人数 */
    private Integer gradedCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
