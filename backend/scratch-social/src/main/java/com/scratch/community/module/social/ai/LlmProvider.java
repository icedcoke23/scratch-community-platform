package com.scratch.community.module.social.ai;

/**
 * LLM 提供商接口
 *
 * <p>支持任何 OpenAI 兼容 API（OpenAI / 通义千问 / DeepSeek / 本地 Ollama 等）。
 * 实现类只需对接 HTTP API，返回统一的 {@link LlmResponse}。
 *
 * <p>配置示例 (application.yml):
 * <pre>
 * scratch:
 *   ai:
 *     enabled: true
 *     provider: openai          # openai / qwen / deepseek / ollama
 *     api-key: sk-xxx
 *     base-url: https://api.openai.com/v1
 *     model: gpt-4o-mini
 *     timeout: 30000
 *     max-tokens: 2000
 * </pre>
 */
public interface LlmProvider {

    /**
     * 获取提供商名称
     */
    String getName();

    /**
     * 发送聊天请求
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return LLM 响应
     * @throws LlmException 调用失败时抛出
     */
    LlmResponse chat(String systemPrompt, String userMessage);

    /**
     * 检查提供商是否可用
     */
    boolean isAvailable();

    /**
     * 是否支持流式输出
     */
    default boolean supportsStreaming() {
        return false;
    }

    /**
     * 流式聊天（SSE）
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @param callback     每收到一段文本时回调
     */
    default void chatStream(String systemPrompt, String userMessage, StreamCallback callback) {
        // 默认实现：调用非流式接口，一次性返回
        LlmResponse response = chat(systemPrompt, userMessage);
        if (response.isSuccess()) {
            callback.onToken(response.getContent());
        }
        callback.onComplete(response);
    }

    /**
     * 流式回调接口
     */
    interface StreamCallback {
        /** 收到一段文本片段 */
        void onToken(String token);
        /** 流式完成 */
        void onComplete(LlmResponse response);
        /** 流式出错 */
        void onError(Throwable error);
    }
}
