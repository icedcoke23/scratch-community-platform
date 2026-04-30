package com.scratch.community.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 内容审核记录实体
 * 对应 database content_audit_log 表
 */
@Getter
@Setter
@TableName(value = "content_audit_log", autoResultMap = true)
public class ContentAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 内容类型: project/comment/homework */
    private String contentType;

    /** 内容 ID */
    private Long contentId;

    /** 审核内容文本 */
    private String contentText;

    /** 状态: pending/passed/rejected */
    private String status;

    /** 拒绝原因 */
    private String reason;

    /** 审核人 ID */
    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
