package com.scratch.community.module.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 协作参与者实体
 */
@Getter
@Setter
@TableName("collab_participant")
public class CollabParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 ID */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 角色: editor / viewer */
    private String role;

    /** 光标 X 坐标 */
    private Integer cursorX;

    /** 光标 Y 坐标 */
    private Integer cursorY;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}
