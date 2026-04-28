package com.scratch.community.judge.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 判题结果
 *
 * <p>统一的判题结果模型，适用于所有题型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResult {

    /** 判题结果 */
    private Verdict verdict;

    /** 运行耗时 (ms) */
    private Long runtimeMs;

    /** 内存使用 (KB) */
    private Long memoryKb;

    /** 得分 (0-100) */
    private Integer score;

    /** 判题详情（JSON 格式） */
    private String detail;

    /** 测试用例结果列表 */
    private List<TestCaseResult> testCaseResults;

    /**
     * 单个测试用例结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        /** 测试用例编号 */
        private int index;
        /** 判题结果 */
        private Verdict verdict;
        /** 运行耗时 (ms) */
        private Long runtimeMs;
        /** 实际输出 */
        private String actual;
        /** 预期输出 */
        private String expected;
        /** 错误信息 */
        private String error;
    }

    /**
     * 创建成功结果
     */
    public static JudgeResult ac(long runtimeMs) {
        return JudgeResult.builder()
                .verdict(Verdict.AC)
                .runtimeMs(runtimeMs)
                .score(100)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static JudgeResult fail(Verdict verdict, String detail) {
        return JudgeResult.builder()
                .verdict(verdict)
                .score(0)
                .detail(detail)
                .build();
    }
}
