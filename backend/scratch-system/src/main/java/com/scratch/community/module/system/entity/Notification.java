package com.scratch.community.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 通知实体
 * 对应 database notification 表
 */
@Getter
@Setter
@TableName(value = "notification", autoResultMap = true)
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者 ID */
    private Long userId;

    /** 通知类型: comment/like/system/homework */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 附加数据 JSON */
    private String data;

    /** 是否已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
