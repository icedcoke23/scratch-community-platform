package com.scratch.community.common.auth;

/**
 * 用户角色枚举
 *
 * <p>统一角色定义，避免散落在各处的字符串 typo 风险。
 * 所有角色校验（RequireRole 注解、Controller 层判断、Service 层判断）
 * 都应使用此枚举。
 *
 * <p>使用方式:
 * <pre>
 * // 注解方式
 * @RequireRole(Role.Names.ADMIN)
 * public R<?> adminOnly() { ... }
 *
 * // 代码方式
 * if (Role.ADMIN.name().equals(user.getRole())) { ... }
 * </pre>
 */
public enum Role {

    /** 学生 */
    STUDENT,

    /** 教师 */
    TEACHER,

    /** 管理员 */
    ADMIN;

    /**
     * 角色名称常量（用于注解参数，注解要求编译期常量）
     */
    public static final class Names {
        public static final String STUDENT = "STUDENT";
        public static final String TEACHER = "TEACHER";
        public static final String ADMIN = "ADMIN";

        private Names() {}
    }

    /**
     * 从字符串安全解析角色，不区分大小写
     *
     * @param roleStr 角色字符串
     * @return 对应的角色枚举，无法匹配时返回 STUDENT（默认角色）
     */
    public static Role fromString(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return STUDENT;
        }
        try {
            return valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STUDENT;
        }
    }

    /**
     * 判断是否为管理员
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 判断是否为教师或管理员
     */
    public boolean isTeacherOrAbove() {
        return this == TEACHER || this == ADMIN;
    }
}
