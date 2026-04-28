package com.scratch.community.module.social.ai;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 配置
 *
 * <p>通过 application.yml 配置:
 * <pre>
 * scratch:
 *   ai:
 *     enabled: true
 *     provider: openai
 *     api-key: sk-xxx
 *     base-url: https://api.openai.com/v1
 *     model: gpt-4o-mini
 *     timeout: 30000
 *     max-tokens: 2000
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(LlmConfig.LlmProperties.class)
@ConditionalOnProperty(prefix = "scratch.ai", name = "enabled", havingValue = "true")
public class LlmConfig {

    @Data
    @ConfigurationProperties(prefix = "scratch.ai")
    public static class LlmProperties {
        /** 是否启用 LLM */
        private boolean enabled = false;
        /** 提供商名称: openai / qwen / deepseek / ollama */
        private String provider = "openai";
        /** API Key */
        private String apiKey = "";
        /** API 基础 URL */
        private String baseUrl = "https://api.openai.com/v1";
        /** 模型名称 */
        private String model = "gpt-4o-mini";
        /** 请求超时 (ms) */
        private int timeout = 30000;
        /** 最大生成 token 数 */
        private int maxTokens = 2000;
    }

    @Bean
    public LlmProvider llmProvider(LlmProperties properties) {
        return new OpenAiCompatibleProvider(
                properties.getProvider(),
                properties.getApiKey(),
                properties.getBaseUrl(),
                properties.getModel(),
                properties.getTimeout(),
                properties.getMaxTokens()
        );
    }
}
