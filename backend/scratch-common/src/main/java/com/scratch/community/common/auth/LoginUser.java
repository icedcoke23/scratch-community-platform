package com.scratch.community.common.auth;

import lombok.Data;

/**
 * 当前登录用户上下文
 */
@Data
public class LoginUser {

    private Long userId;
    private String username;
    private String role;

    /**
     * ThreadLocal 存储当前请求的登录用户
     */
    private static final ThreadLocal<LoginUser> CURRENT = new ThreadLocal<>();

    public static void set(LoginUser user) {
        CURRENT.set(user);
    }

    public static LoginUser get() {
        return CURRENT.get();
    }

    public static Long getUserId() {
        LoginUser user = CURRENT.get();
        return user != null ? user.getUserId() : null;
    }

    public static void remove() {
        CURRENT.remove();
    }

    /**
     * 是否为教师
     */
    public boolean isTeacher() {
        return "TEACHER".equals(role);
    }

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
