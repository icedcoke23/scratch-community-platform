package com.scratch.community.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 班级实体
 */
@Getter
@Setter
@TableName("class")
public class ClassRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Long teacherId;

    private String inviteCode;

    private String grade;

    private Integer studentCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
