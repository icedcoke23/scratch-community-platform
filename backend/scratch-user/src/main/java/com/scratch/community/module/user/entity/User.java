package com.scratch.community.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Getter
@Setter
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    private String username;

    private String password;

    private String nickname;

    private String avatarUrl;

    private String bio;

    private String email;

    private String role;

    private Integer status;

    /** 总积分 */
    private Integer points;

    /** 等级 */
    private Integer level;

    /** 登录次数 */
    private Integer loginCount;

    /** 当前有效 Refresh Token */
    private String refreshToken;

    /** Refresh Token 过期时间 */
    private LocalDateTime refreshTokenExpiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
