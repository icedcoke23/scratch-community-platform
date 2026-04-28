package com.scratch.community.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户第三方登录绑定实体
 * 存储用户与微信/QQ等第三方平台的绑定关系
 */
@Getter
@Setter
@TableName("user_oauth")
public class UserOAuth {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 第三方平台: wechat / qq */
    private String provider;

    /** 第三方平台的唯一标识 (openid / unionid) */
    private String openId;

    /** Union ID（微信开放平台可选，用于跨应用识别） */
    private String unionId;

    /** 第三方平台返回的昵称 */
    private String nickname;

    /** 第三方平台返回的头像 */
    private String avatarUrl;

    /** Access Token（短期有效） */
    private String accessToken;

    /** Refresh Token（用于刷新 Access Token） */
    private String refreshToken;

    /** Token 过期时间 */
    private LocalDateTime tokenExpiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
