package com.scratch.community.module.social.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {

    /** 响应内容 */
    private String content;

    /** 使用的 token 数量 */
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    /** 模型名称 */
    private String model;

    /** 响应耗时 (ms) */
    private Long durationMs;

    /** 是否成功 */
    public boolean isSuccess() {
        return content != null && !content.isBlank();
    }
}
