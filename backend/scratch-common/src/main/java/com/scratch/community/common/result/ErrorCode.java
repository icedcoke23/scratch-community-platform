package com.scratch.community.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ========== 通用 ==========
    SUCCESS(0, "success"),
    SYSTEM_ERROR(9999, "系统异常"),
    PARAM_ERROR(9998, "参数错误"),
    UNAUTHORIZED(9997, "未登录"),
    FORBIDDEN(9996, "无权限"),

    // ========== 用户模块 10000-19999 ==========
    USER_EXISTS(10001, "用户名已存在"),
    PASSWORD_ERROR(10002, "密码错误"),
    TOKEN_EXPIRED(10003, "Token 已过期"),
    USER_NO_PERMISSION(10004, "无权限操作"),
    USER_NOT_FOUND(10005, "用户不存在"),
    CLASS_NOT_FOUND(10006, "班级不存在"),
    INVITE_CODE_INVALID(10007, "邀请码无效"),

    // ========== 创作模块 20000-29999 ==========
    PROJECT_NOT_FOUND(20001, "项目不存在"),
    PROJECT_NO_PERMISSION(20002, "无权操作此项目"),
    SB3_FORMAT_ERROR(20003, "sb3 文件格式错误"),
    FILE_UPLOAD_ERROR(20004, "文件上传失败"),

    // ========== 社区模块 30000-39999 ==========
    PROJECT_NOT_PUBLISHED(30001, "作品未发布"),
    ALREADY_LIKED(30002, "已经点赞"),
    COMMENT_INVALID(30003, "评论内容违规"),
    PROJECT_NOT_EXIST(30004, "作品不存在"),

    // ========== 判题模块 40000-49999 ==========
    PROBLEM_NOT_FOUND(40001, "题目不存在"),
    JUDGE_TIMEOUT(40002, "判题超时"),
    SANDBOX_UNAVAILABLE(40003, "沙箱不可用"),
    SUBMISSION_DUPLICATE(40004, "请勿重复提交"),

    // ========== 教室模块 50000-59999 ==========
    HOMEWORK_DEADLINE_PASSED(50001, "作业已截止"),
    HOMEWORK_ALREADY_SUBMITTED(50002, "已经提交"),
    HOMEWORK_NOT_FOUND(50003, "作业不存在"),

    // ========== 系统模块 60000-69999 ==========
    CONTENT_AUDIT_FAIL(60001, "内容包含违规信息"),
    CONFIG_NOT_FOUND(60002, "配置不存在"),
    ADMIN_NO_PERMISSION(60003, "无管理权限");

    private final int code;
    private final String msg;
}
