package com.scratch.community.module.social.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenAI 兼容 LLM 提供商
 *
 * <p>支持所有 OpenAI 兼容 API:
 * <ul>
 *   <li>OpenAI: https://api.openai.com/v1</li>
 *   <li>通义千问: https://dashscope.aliyuncs.com/compatible-mode/v1</li>
 *   <li>DeepSeek: https://api.deepseek.com/v1</li>
 *   <li>Ollama: http://localhost:11434/v1</li>
 *   <li>任何 OpenAI 兼容服务</li>
 * </ul>
 */
@Slf4j
public class OpenAiCompatibleProvider implements LlmProvider {

    private final String name;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeoutMs;
    private final int maxTokens;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /** 可用性缓存：避免每次调用都发真实 API 请求 */
    private volatile Boolean cachedAvailability = null;
    private volatile long availabilityCheckedAt = 0;
    private static final long AVAILABILITY_CACHE_TTL_MS = 5 * 60 * 1000; // 5 分钟缓存

    public OpenAiCompatibleProvider(String name, String apiKey, String baseUrl,
                                     String model, int timeoutMs, int maxTokens) {
        this.name = name;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.timeoutMs = timeoutMs;
        this.maxTokens = maxTokens;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(10000))
                .build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LlmResponse chat(String systemPrompt, String userMessage) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.7);

            ArrayNode messages = requestBody.putArray("messages");
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode systemMsg = messages.addObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt);
            }
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 发送请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long duration = System.currentTimeMillis() - startTime;

            if (response.statusCode() != 200) {
                throw new LlmException(name, response.statusCode(),
                        "API 返回错误: " + response.statusCode() + " - " + response.body());
            }

            // 解析响应
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LlmException(name, "API 返回空响应");
            }

            String content = choices.get(0).get("message").get("content").asText();

            // 解析 token 使用量
            JsonNode usage = root.get("usage");
            Integer promptTokens = usage != null && usage.has("prompt_tokens")
                    ? usage.get("prompt_tokens").asInt() : null;
            Integer completionTokens = usage != null && usage.has("completion_tokens")
                    ? usage.get("completion_tokens").asInt() : null;
            Integer totalTokens = usage != null && usage.has("total_tokens")
                    ? usage.get("total_tokens").asInt() : null;

            log.info("LLM 调用成功: provider={}, model={}, tokens={}, duration={}ms",
                    name, model, totalTokens, duration);

            return LlmResponse.builder()
                    .content(content)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .model(model)
                    .durationMs(duration)
                    .build();

        } catch (LlmException e) {
            throw e;
        } catch (java.net.ConnectException | java.net.http.HttpConnectTimeoutException e) {
            throw new LlmException(name, "连接超时: " + e.getMessage(), e);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new LlmException(name, "请求超时: " + timeoutMs + "ms", e);
        } catch (Exception e) {
            throw new LlmException(name, "调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public void chatStream(String systemPrompt, String userMessage, StreamCallback callback) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", true);

            ArrayNode messages = requestBody.putArray("messages");
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode systemMsg = messages.addObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt);
            }
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            // 流式读取 SSE 响应
            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String body = new String(response.body().readAllBytes());
                callback.onError(new LlmException(name, response.statusCode(), "API 错误: " + body));
                return;
            }

            StringBuilder fullContent = new StringBuilder();
            int totalTokens = 0;

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    try {
                        JsonNode chunk = objectMapper.readTree(data);
                        JsonNode choices = chunk.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            JsonNode delta = choices.get(0).get("delta");
                            if (delta != null && delta.has("content")) {
                                String token = delta.get("content").asText();
                                fullContent.append(token);
                                callback.onToken(token);
                            }
                        }
                        // 解析 token 使用量（如果有）
                        if (chunk.has("usage")) {
                            JsonNode usage = chunk.get("usage");
                            if (usage.has("total_tokens")) {
                                totalTokens = usage.get("total_tokens").asInt();
                            }
                        }
                    } catch (Exception ignored) {
                        // 解析单行失败，跳过
                    }
                }
            }

            LlmResponse result = LlmResponse.builder()
                    .content(fullContent.toString())
                    .totalTokens(totalTokens > 0 ? totalTokens : null)
                    .model(model)
                    .build();
            callback.onComplete(result);

        } catch (LlmException e) {
            callback.onError(e);
        } catch (Exception e) {
            callback.onError(new LlmException(name, "流式调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public boolean isAvailable() {
        // 快速检查：API Key 未配置
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("CHANGE_ME")) {
            return false;
        }

        // 缓存命中：5 分钟内不重复检查
        long now = System.currentTimeMillis();
        if (cachedAvailability != null && (now - availabilityCheckedAt) < AVAILABILITY_CACHE_TTL_MS) {
            return cachedAvailability;
        }

        try {
            // 简单的健康检查：尝试发一个最小请求
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 5);
            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode msg = messages.addObject();
            msg.put("role", "user");
            msg.put("content", "hi");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofMillis(5000))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            cachedAvailability = response.statusCode() == 200;
        } catch (Exception e) {
            cachedAvailability = false;
            log.warn("LLM 健康检查失败: provider={}, error={}", name, e.getMessage());
        }

        availabilityCheckedAt = now;
        return cachedAvailability;
    }
}
