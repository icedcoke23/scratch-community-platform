package com.scratch.community.judge.core;

import com.scratch.community.judge.core.model.JudgeResult;

/**
 * 判题引擎接口
 *
 * <p>所有判题引擎（Scratch 编程题、选择题、判断题）都应实现此接口。
 * 提供统一的判题入口和结果模型。
 */
public interface JudgeEngine {

    /**
     * 执行判题
     *
     * @param request 判题请求
     * @return 判题结果
     */
    JudgeResult judge(JudgeRequest request);

    /**
     * 判题请求
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class JudgeRequest {
        /** 提交 ID */
        private Long submissionId;
        /** 题目 ID */
        private Long problemId;
        /** 题目类型: scratch_algo / choice / true_false */
        private String problemType;
        /** 用户提交的答案（选择题/判断题） */
        private String answer;
        /** sb3 文件 URL（Scratch 编程题） */
        private String sb3Url;
        /** 预期输出（Scratch 编程题） */
        private String expectedOutput;
        /** 超时时间 (ms) */
        private long timeoutMs = 30000;
    }
}
