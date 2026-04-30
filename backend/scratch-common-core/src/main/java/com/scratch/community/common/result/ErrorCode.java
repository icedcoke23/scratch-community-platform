package com.scratch.community.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 *
 * <p>编码规范:
 * <ul>
 *   <li>通用: 0, 9994-9999</li>
 *   <li>用户模块: 10000-19999</li>
 *   <li>创作模块: 20000-29999</li>
 *   <li>社区模块: 30000-39999</li>
 *   <li>判题模块: 40000-49999</li>
 *   <li>教室模块: 50000-59999</li>
 *   <li>系统模块: 60000-69999</li>
 * </ul>
 *
 * <p>前端通过 {@code code} 字段判断错误类型，展示对应的用户提示。
 * 新增错误码时请遵循上述编码区间，并同步更新前端 i18n 映射。
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
    REFRESH_TOKEN_EXPIRED(9995, "Refresh Token 已过期"),
    REFRESH_TOKEN_INVALID(9994, "Refresh Token 无效"),
    RATE_LIMITED(9993, "请求过于频繁，请稍后再试"),
    RESOURCE_NOT_FOUND(9992, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(9991, "请求方法不支持"),
    IDEMPOTENT_CONFLICT(9990, "重复请求"),

    // ========== 用户模块 10000-19999 ==========
    USER_EXISTS(10001, "用户名已存在"),
    PASSWORD_ERROR(10002, "密码错误"),
    TOKEN_EXPIRED(10003, "Token 已过期"),
    USER_NO_PERMISSION(10004, "无权限操作"),
    USER_NOT_FOUND(10005, "用户不存在"),
    CLASS_NOT_FOUND(10006, "班级不存在"),
    INVITE_CODE_INVALID(10007, "邀请码无效"),
    THIRD_PARTY_AUTH_FAILED(10008, "第三方登录失败"),

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
    NOT_LIKED(30005, "未点赞"),

    // ========== 判题模块 40000-49999 ==========
    PROBLEM_NOT_FOUND(40001, "题目不存在"),
    PROBLEM_NOT_PUBLISHED(40002, "题目未发布"),
    JUDGE_TIMEOUT(40003, "判题超时"),
    SANDBOX_UNAVAILABLE(40004, "沙箱不可用"),
    SUBMISSION_DUPLICATE(40005, "请勿重复提交"),
    SUBMISSION_NOT_FOUND(40006, "提交记录不存在"),

    // ========== 教室模块 50000-59999 ==========
    HOMEWORK_DEADLINE_PASSED(50001, "作业已截止"),
    HOMEWORK_ALREADY_SUBMITTED(50002, "已经提交"),
    HOMEWORK_NOT_FOUND(50003, "作业不存在"),
    HOMEWORK_NOT_PUBLISHED(50004, "作业未发布"),

    // ========== 系统模块 60000-69999 ==========
    CONTENT_AUDIT_FAIL(60001, "内容包含违规信息"),
    CONFIG_NOT_FOUND(60002, "配置不存在"),
    ADMIN_NO_PERMISSION(60003, "无管理权限");

    private final int code;
    private final String msg;
}
