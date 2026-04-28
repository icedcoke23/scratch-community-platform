package com.scratch.community.common.result;

import lombok.Data;

/**
 * 统一返回体
 *
 * <p>所有 API 接口统一使用此包装类返回数据，前端通过 {@code code} 判断业务状态：
 * <ul>
 *   <li>{@code code == 0} 表示成功</li>
 *   <li>{@code code != 0} 表示失败，{@code msg} 包含错误描述</li>
 * </ul>
 *
 * <p>响应示例:
 * <pre>
 * {
 *   "code": 0,
 *   "msg": "success",
 *   "data": { ... },
 *   "timestamp": 1714000000000
 * }
 * </pre>
 */
@Data
public class R<T> {

    /** 业务状态码，0 表示成功 */
    private int code;

    /** 业务消息 */
    private String msg;

    /** 业务数据 */
    private T data;

    /** 服务端时间戳（ms），用于前端时间校准和缓存控制 */
    private long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 成功 ====================

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMsg("success");
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    /**
     * 带自定义消息的成功响应（适用于操作类接口，如"创建成功"）
     */
    public static <T> R<T> ok(String msg, T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }

    // ==================== 失败 ====================

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        R<T> r = new R<>();
        r.setCode(errorCode.getCode());
        r.setMsg(errorCode.getMsg());
        return r;
    }

    /**
     * 带附加数据的失败响应（适用于需要返回部分信息的场景）
     */
    public static <T> R<T> fail(ErrorCode errorCode, T data) {
        R<T> r = new R<>();
        r.setCode(errorCode.getCode());
        r.setMsg(errorCode.getMsg());
        r.setData(data);
        return r;
    }
}
