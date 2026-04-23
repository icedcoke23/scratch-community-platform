package com.scratch.community.module.user.vo;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginVO {

    private String token;
    private UserVO user;

    @Data
    public static class UserVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;
        private String role;
    }
}
