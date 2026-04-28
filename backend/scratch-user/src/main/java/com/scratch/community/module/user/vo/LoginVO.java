package com.scratch.community.module.user.vo;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginVO {

    private String token;
    private String refreshToken;
    private UserVO userInfo;
}
