package com.scratch.community.module.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 点赞实体
 * 对应 database project_like 表
 */
@Getter
@Setter
@TableName("project_like")
public class ProjectLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long projectId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
