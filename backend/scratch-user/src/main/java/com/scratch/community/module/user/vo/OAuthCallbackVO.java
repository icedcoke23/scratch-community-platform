package com.scratch.community.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * OAuth 登录结果 VO
 */
@Data
@Schema(description = "第三方登录结果")
public class OAuthCallbackVO {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "是否为新注册用户")
    private Boolean newUser;

    @Schema(description = "用户信息")
    private UserVO userInfo;
}
