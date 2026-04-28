package com.scratch.community.module.user.oauth;

import lombok.Data;

/**
 * 第三方平台用户信息
 */
@Data
public class OAuthUserInfo {

    /** 第三方平台唯一标识 */
    private String openId;

    /** Union ID（跨应用） */
    private String unionId;

    /** 平台名称 */
    private String provider;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatarUrl;

    /** Access Token */
    private String accessToken;

    /** Refresh Token */
    private String refreshToken;

    /** Token 过期秒数 */
    private Integer expiresIn;
}
