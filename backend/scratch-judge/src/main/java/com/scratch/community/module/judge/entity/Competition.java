package com.scratch.community.module.judge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 竞赛实体
 */
@Getter
@Setter
@TableName("competition")
public class Competition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 竞赛标题 */
    private String title;

    /** 竞赛描述 */
    private String description;

    /** 创建者 ID */
    private Long creatorId;

    /**
     * 竞赛类型:
     * - TIMED: 限时赛（固定时间段）
     * - RATED: 排名赛（影响 Rating）
     */
    private String type;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 题目 ID 列表 (JSON) */
    private String problemIds;

    /** 每题分值 (JSON) */
    private String problemScores;

    /** 总分 */
    private Integer totalScore;

    /** 参赛人数 */
    private Integer participantCount;

    /**
     * 状态:
     * - DRAFT: 草稿
     * - PUBLISHED: 已发布（可报名）
     * - RUNNING: 进行中
     * - ENDED: 已结束
     */
    private String status;

    /** 是否公开（true=所有人可参加，false=邀请制） */
    private Boolean isPublic;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
