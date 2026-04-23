package com.scratch.community.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级学生关系实体
 */
@Data
@TableName("class_student")
public class ClassStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;

    private Long studentId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}
