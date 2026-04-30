package com.scratch.community.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 积分变动记录
 */
@Getter
@Setter
@TableName("point_log")
public class PointLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /**
     * 变动类型:
     * - DAILY_CHECKIN: 每日签到
     * - PUBLISH_PROJECT: 发布项目
     * - RECEIVE_LIKE: 收到点赞
     * - AC_SUBMISSION: 判题通过
     * - COMPLETE_HOMEWORK: 完成作业
     * - ADMIN_ADJUST: 管理员调整
     */
    private String type;

    /** 变动积分（正数增加，负数扣减） */
    private Integer points;

    /** 变动后总积分 */
    private Integer totalPoints;

    /** 关联对象类型: project/submission/homework/user */
    private String refType;

    /** 关联对象 ID */
    private Long refId;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
