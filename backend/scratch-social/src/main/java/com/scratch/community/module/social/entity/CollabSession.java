package com.scratch.community.module.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 协作会话实体
 * 一个项目同时只有一个活跃的协作会话
 */
@Getter
@Setter
@TableName("collab_session")
public class CollabSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目 ID */
    private Long projectId;

    /** 会话创建者（项目所有者或教师） */
    private Long ownerId;

    /** 会话状态: active / closed */
    private String status;

    /** 最大编辑者数量 */
    private Integer maxEditors;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
