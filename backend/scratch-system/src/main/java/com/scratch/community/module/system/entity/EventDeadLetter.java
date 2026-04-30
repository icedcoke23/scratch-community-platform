package com.scratch.community.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件死信队列表实体
 * 用于存储处理失败的事件，支持重试和人工干预
 */
@Data
@TableName("event_dead_letter")
public class EventDeadLetter {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 事件类型：PointEvent/NotificationEvent 等
     */
    private String eventType;

    /**
     * 事件完整数据（JSON 格式）
     */
    @TableField(typeHandler = org.apache.ibatis.type.JdbcType.VARCHAR.class)
    private String eventData;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryAt;

    /**
     * 状态：PENDING/RETRYING/SUCCESS/FAILED
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 解决时间
     */
    private LocalDateTime resolvedAt;

    /**
     * 解决人
     */
    private String resolvedBy;

    /**
     * 解决说明
     */
    private String resolutionNote;
}
