package com.scratch.community.common.auth;

import java.lang.annotation.*;

/**
 * 角色校验注解
 * 用法: @RequireRole("TEACHER") 或 @RequireRole({"TEACHER", "ADMIN"})
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value();
}
