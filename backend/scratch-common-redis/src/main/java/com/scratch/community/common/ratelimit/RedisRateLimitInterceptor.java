package com.scratch.community.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scratch.community.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Redis 分布式限流拦截器
 *
 * <p>基于 Redis + Lua 的滑动窗口限流，支持多实例部署。
 * 限流粒度: IP + URI（可扩展为用户级别）
 *
 * <p>响应头:
 * - X-RateLimit-Limit: 窗口内最大请求数
 * - X-RateLimit-Remaining: 剩余请求数
 * - Retry-After: 超限时的重试等待秒数
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RedisRateLimiter.class)
public class RedisRateLimitInterceptor implements HandlerInterceptor {

    private final RedisRateLimiter rateLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 默认窗口大小（秒） */
    private static final int DEFAULT_WINDOW = 60;
    /** 默认最大请求数 */
    private static final int DEFAULT_MAX_REQUESTS = 120;

    /** 端点级限流规则 */
    private record RateLimitRule(int maxRequests, int windowSeconds) {}

    private static final java.util.Map<String, RateLimitRule> ENDPOINT_RULES = new java.util.LinkedHashMap<>();

    static {
        // 登录/注册：10 次/分钟（防暴力破解）
        ENDPOINT_RULES.put("/api/user/login", new RateLimitRule(10, 60));
        ENDPOINT_RULES.put("/api/user/register", new RateLimitRule(10, 60));
        // 判题：30 次/分钟
        ENDPOINT_RULES.put("/api/judge/", new RateLimitRule(30, 60));
        // AI 流式点评：5 次/分钟（更严格，防止 GPU 资源滥用）
        ENDPOINT_RULES.put("/api/ai-review/project/", new RateLimitRule(5, 60));
        // AI 点评：20 次/分钟
        ENDPOINT_RULES.put("/api/ai-review/", new RateLimitRule(20, 60));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        String uri = request.getRequestURI();

        // 不限流的路径
        if (isExcluded(uri)) {
            return true;
        }

        // 查找端点级规则
        RateLimitRule rule = findEndpointRule(uri);
        int maxRequests = rule != null ? rule.maxRequests() : DEFAULT_MAX_REQUESTS;
        int windowSeconds = rule != null ? rule.windowSeconds() : DEFAULT_WINDOW;

        // 构建限流 key（端点级规则用端点 key，全局用归一化 URI）
        String key = rule != null
                ? "rate:ip:" + ip + ":" + rule.maxRequests() + ":" + uri.split("/api/")[1].split("/")[0]
                : "rate:ip:" + ip + ":" + normalizeUri(uri);

        // 检查限流
        RedisRateLimiter.RateLimitResult result = rateLimiter.tryAcquire(key, windowSeconds, maxRequests);

        // 设置响应头
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, result.remaining())));

        if (!result.allowed()) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf((result.retryAfterMs() + 999) / 1000));

            String body = objectMapper.writeValueAsString(
                    R.fail(429, "请求过于频繁，请 " + ((result.retryAfterMs() + 999) / 1000) + " 秒后重试"));

            response.getWriter().write(body);
            log.warn("IP 限流: ip={}, uri={}, retryAfter={}ms", ip, uri, result.retryAfterMs());
            return false;
        }

        return true;
    }

    /**
     * 获取客户端真实 IP
     *
     * <p>安全策略: 优先信任 Nginx 设置的 X-Real-IP（不可被客户端伪造），
     * X-Forwarded-For 取最后一个非匿名值（最接近服务端的代理 IP），
     * 避免客户端伪造第一个值绕过限流。
     */
    private String getClientIp(HttpServletRequest request) {
        // 优先: X-Real-IP（由 Nginx 反向代理设置，不可被客户端伪造）
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        // 降级: X-Forwarded-For（取最后一个非匿名值，即最接近服务端的代理 IP）
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] ips = forwarded.split(",");
            for (int i = ips.length - 1; i >= 0; i--) {
                String candidate = ips[i].trim();
                if (!"unknown".equalsIgnoreCase(candidate) && !candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        // 兜底: TCP 连接地址
        ip = request.getRemoteAddr();
        // 处理 IPv6 本地地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * URI 归一化（去掉 ID 参数，减少 key 空间）
     * /api/project/123 → /api/project/{id}
     */
    private String normalizeUri(String uri) {
        // 简单的数字 ID 替换
        return uri.replaceAll("/\\d+", "/{id}");
    }

    /**
     * 查找端点级限流规则（最长前缀匹配）
     */
    private RateLimitRule findEndpointRule(String uri) {
        RateLimitRule matched = null;
        int longestMatch = 0;
        for (var entry : ENDPOINT_RULES.entrySet()) {
            String pattern = entry.getKey();
            if (uri.startsWith(pattern) && pattern.length() > longestMatch) {
                matched = entry.getValue();
                longestMatch = pattern.length();
            }
        }
        return matched;
    }

    /**
     * 排除不限流的路径
     */
    private boolean isExcluded(String uri) {
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/favicon")
                || uri.equals("/");
    }
}
