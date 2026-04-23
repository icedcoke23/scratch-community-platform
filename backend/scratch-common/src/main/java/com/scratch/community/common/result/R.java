package com.scratch.community.common.result;

import lombok.Data;

/**
 * 统一返回体
 */
@Data
public class R<T> {

    private int code;
    private String msg;
    private T data;

    private R() {}

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
}
