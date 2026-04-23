package com.scratch.community.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级实体
 */
@Data
@TableName("class")
public class ClassRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

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
