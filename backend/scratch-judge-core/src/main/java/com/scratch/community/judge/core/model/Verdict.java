package com.scratch.community.judge.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 判题结果枚举
 *
 * <p>所有判题模块（Scratch 编程题、选择题、判断题）统一使用此枚举表示结果。
 */
@Getter
@AllArgsConstructor
public enum Verdict {

    /** 待判题 */
    PENDING("PENDING", "待判题"),
    /** 答案正确 */
    AC("AC", "答案正确"),
    /** 答案错误 */
    WA("WA", "答案错误"),
    /** 运行超时 */
    TLE("TLE", "运行超时"),
    /** 运行时错误 */
    RE("RE", "运行时错误"),
    /** 内存超限 */
    MLE("MLE", "内存超限"),
    /** 编译错误 */
    CE("CE", "编译错误"),
    /** 系统错误 */
    SE("SE", "系统错误");

    private final String code;
    private final String description;

    /**
     * 是否为最终状态（非 PENDING）
     */
    public boolean isFinal() {
        return this != PENDING;
    }

    /**
     * 是否为通过状态
     */
    public boolean isAccepted() {
        return this == AC;
    }

    /**
     * 从字符串解析 Verdict，忽略大小写
     */
    public static Verdict fromString(String s) {
        if (s == null) return RE;
        for (Verdict v : values()) {
            if (v.code.equalsIgnoreCase(s.trim())) return v;
        }
        return RE;
    }
}
