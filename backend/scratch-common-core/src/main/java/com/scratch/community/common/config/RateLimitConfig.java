package com.scratch.community.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口限流配置
 *
 * <h2>限流算法说明：滑动窗口计数器（Sliding Window Counter）</h2>
 *
 * <p><b>当前实现：滑动窗口计数器（Sliding Window Counter）</b>
 * <ul>
 *   <li>将窗口细分为 10 个子窗口（如 60 秒分为 10 个 6 秒），按时间槽位滚动统计请求数</li>
 *   <li>通过清除过期子窗口并累加当前有效子窗口的计数来判断是否超限</li>
 *   <li>优点：有效解决固定窗口在边界处的 <b>2x 突发流量</b>问题，兼顾精度和内存开销</li>
 *   <li>内存开销：每个 key 存储 subWindowCount+1 个 long 值（10 个计数器 + 1 个槽位索引）</li>
 * </ul>
 *
 * <p><b>与其他算法对比：</b>
 * <ul>
 *   <li>固定窗口（Fixed Window）：实现简单，但在窗口边界处可能产生 2x 突发流量</li>
 *   <li>滑动窗口日志（Sliding Window Log）：最精确，但需存储每个请求的时间戳，内存开销大</li>
 *   <li>滑动窗口计数器（当前）：折中方案，精度好且内存可控</li>
 * </ul>
 *
 * <p>限制规则:
 * <ul>
 *   <li>默认: 60 次/分钟</li>
 *   <li>登录接口: 10 次/分钟（防暴力破解）</li>
 *   <li>判题接口: 30 次/分钟</li>
 *   <li>AI 点评: 20 次/分钟</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnMissingBean(name = "redisRateLimiter")
public class RateLimitConfig implements WebMvcConfigurer {

    /** 全局限流器（按 IP） */
    private final RateLimiter globalLimiter = new RateLimiter(60, 60000);
    /** 登录限流器（按 IP） */
    private final RateLimiter loginLimiter = new RateLimiter(10, 60000);
    /** 判题限流器（按 IP） */
    private final RateLimiter judgeLimiter = new RateLimiter(30, 60000);
    /** AI 点评限流器（按 IP，每分钟 20 次） */
    private final RateLimiter aiReviewLimiter = new RateLimiter(20, 60000);
    /** AI 流式点评限流器（按 IP，每分钟 5 次，更严格） */
    private final RateLimiter aiStreamLimiter = new RateLimiter(5, 60000);

    /** 注解限流器缓存: key = "maxRequests:windowMs" -> RateLimiter */
    private final Map<String, RateLimiter> annotationLimiters = new ConcurrentHashMap<>();

    /** 所有限流器的集合，用于定时清理 */
    private final RateLimiter[] allLimiters = {
            globalLimiter, loginLimiter, judgeLimiter, aiReviewLimiter, aiStreamLimiter
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注解限流（最高优先级，在全局限流之前检查）
        registry.addInterceptor(new AnnotationRateLimitInterceptor(annotationLimiters))
                .addPathPatterns("/api/**")
                .order(0);

        // 全局限流
        registry.addInterceptor(new RateLimitInterceptor(globalLimiter))
                .addPathPatterns("/api/**")
                .order(1); // 最高优先级

        // 登录接口限流（覆盖新旧两套路径）
        registry.addInterceptor(new RateLimitInterceptor(loginLimiter))
                .addPathPatterns(
                        "/api/user/login", "/api/user/register",
                        "/api/v1/user/login", "/api/v1/user/register"
                )
                .order(2);

        // 判题接口限流（覆盖新旧两套路径）
        registry.addInterceptor(new RateLimitInterceptor(judgeLimiter))
                .addPathPatterns("/api/judge/**", "/api/v1/judge/**")
                .order(2);

        // AI 流式点评限流（更严格，防止 GPU 资源滥用）
        registry.addInterceptor(new RateLimitInterceptor(aiStreamLimiter))
                .addPathPatterns(
                        "/api/ai-review/project/*/stream",
                        "/api/v1/ai-review/project/*/stream"
                )
                .order(2);

        // AI 点评限流
        registry.addInterceptor(new RateLimitInterceptor(aiReviewLimiter))
                .addPathPatterns("/api/ai-review/**", "/api/v1/ai-review/**")
                .order(3);
    }

    /**
     * 定时清理过期的限流窗口，防止内存泄漏
     *
     * <p>每 60 秒执行一次，遍历所有限流器的 windows Map，
     * 移除已过期（超过 2 倍窗口时长）的 Window 条目。
     * 使用 ConcurrentHashMap 的 iterator 遍历，支持并发安全的删除操作。
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredWindows() {
        int totalCleaned = 0;
        for (RateLimiter limiter : allLimiters) {
            totalCleaned += limiter.cleanup();
        }
        // 清理注解限流器
        for (RateLimiter limiter : annotationLimiters.values()) {
            totalCleaned += limiter.cleanup();
        }
        if (totalCleaned > 0) {
            log.debug("限流器过期窗口清理完成，共清理 {} 个条目", totalCleaned);
        }
    }

    /**
     * 注解限流拦截器
     *
     * <p>检查 Controller 方法上的 {@link RateLimit} 注解，
     * 如果存在则使用注解配置的限流参数，否则放行（交给后续全局拦截器处理）。
     */
    class AnnotationRateLimitInterceptor implements HandlerInterceptor {
        private final Map<String, RateLimiter> limiterCache;

        AnnotationRateLimitInterceptor(Map<String, RateLimiter> limiterCache) {
            this.limiterCache = limiterCache;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (!(handler instanceof HandlerMethod handlerMethod)) {
                return true;
            }

            RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (rateLimit == null) {
                return true; // 无注解，交给后续拦截器处理
            }

            // 获取或创建限流器（按注解参数缓存）
            String cacheKey = rateLimit.maxRequests() + ":" + rateLimit.windowSeconds() * 1000L;
            RateLimiter limiter = limiterCache.computeIfAbsent(cacheKey,
                    k -> new RateLimiter(rateLimit.maxRequests(), rateLimit.windowSeconds() * 1000L));

            // 构建限流 key
            String key = getClientIp(request);
            if (!rateLimit.keyPrefix().isEmpty()) {
                key = rateLimit.keyPrefix() + ":" + key;
            }

            RateLimiter.AcquireResult result = limiter.tryAcquireWithInfo(key);

            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.maxRequests()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, result.remaining)));

            if (!result.acquired) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds));
                response.getWriter().write("{\"code\":42900,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}");
                return false;
            }
            return true;
        }

        private String getClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Real-IP");
            if (ip != null && !ip.isBlank()) return ip.trim();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] ips = forwarded.split(",");
                for (int i = ips.length - 1; i >= 0; i--) {
                    String candidate = ips[i].trim();
                    if (!"unknown".equalsIgnoreCase(candidate) && !candidate.isEmpty()) return candidate;
                }
            }
            return request.getRemoteAddr();
        }
    }

    /**
     * 限流拦截器
     */
    static class RateLimitInterceptor implements HandlerInterceptor {
        private final RateLimiter limiter;

        RateLimitInterceptor(RateLimiter limiter) {
            this.limiter = limiter;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String key = getClientIp(request);
            RateLimiter.AcquireResult result = limiter.tryAcquireWithInfo(key);

            // 设置标准限流响应头
            response.setHeader("X-RateLimit-Limit", String.valueOf(limiter.getMaxRequests()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, result.remaining)));

            if (!result.acquired) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds));
                response.getWriter().write("{\"code\":42900,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}");
                return false;
            }
            return true;
        }

        /**
         * 获取客户端真实 IP
         *
         * <p>安全策略: 优先信任 Nginx 设置的 X-Real-IP（不可伪造），
         * X-Forwarded-For 的第一个值可能被客户端伪造，仅作为降级方案。
         * 如果两者都不可用，使用 TCP 连接的 remoteAddr。
         */
        private String getClientIp(HttpServletRequest request) {
            // 优先: X-Real-IP（由 Nginx 反向代理设置，不可被客户端伪造）
            String ip = request.getHeader("X-Real-IP");
            if (ip != null && !ip.isBlank()) {
                return ip.trim();
            }
            // 降级: X-Forwarded-For（取最后一个非匿名值，即最接近服务端的代理 IP）
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // 格式: client, proxy1, proxy2 → 取最后一个
                String[] ips = forwarded.split(",");
                for (int i = ips.length - 1; i >= 0; i--) {
                    String candidate = ips[i].trim();
                    if (!"unknown".equalsIgnoreCase(candidate) && !candidate.isEmpty()) {
                        return candidate;
                    }
                }
            }
            // 兜底: TCP 连接地址
            return request.getRemoteAddr();
        }
    }

    /**
     * 滑动窗口计数器限流器（Sliding Window Counter）
     *
     * <p>将窗口细分为多个子窗口，按时间槽位滚动统计，避免固定窗口在边界处的 2x 突发问题。
     * 使用 ConcurrentHashMap 存储每个 key 的子窗口计数数组。
     */
    static class RateLimiter {

        /**
         * 限流结果（携带元信息，用于设置标准限流响应头）
         */
        static class AcquireResult {
            final boolean acquired;
            final int remaining;
            final long retryAfterSeconds;

            AcquireResult(boolean acquired, int remaining, long retryAfterSeconds) {
                this.acquired = acquired;
                this.remaining = remaining;
                this.retryAfterSeconds = retryAfterSeconds;
            }
        }

        private final int maxRequests;
        private final long windowMs;
        private final int subWindowCount;
        /** Key -> counters[subWindowCount] = lastSubWindowIndex */
        private final Map<String, long[]> windows = new ConcurrentHashMap<>();

        RateLimiter(int maxRequests, long windowMs) {
            this(maxRequests, windowMs, 10);
        }

        RateLimiter(int maxRequests, long windowMs, int subWindowCount) {
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
            this.subWindowCount = subWindowCount;
        }

        int getMaxRequests() {
            return maxRequests;
        }

        boolean tryAcquire(String key) {
            return tryAcquireWithInfo(key).acquired;
        }

        AcquireResult tryAcquireWithInfo(String key) {
            long now = System.currentTimeMillis();
            long subWindowMs = windowMs / subWindowCount;
            long currentSlot = (now / subWindowMs) % subWindowCount;

            long[] counters = windows.compute(key, (k, v) -> {
                if (v == null) {
                    long[] arr = new long[subWindowCount + 1];
                    arr[subWindowCount] = currentSlot;
                    return arr;
                }
                long lastSlot = v[subWindowCount];
                long slotsToAdvance = (currentSlot - lastSlot + subWindowCount) % subWindowCount;
                if (slotsToAdvance == 0 && v[(int) currentSlot] > 0) {
                    return v; // same slot, no shift needed
                }
                if (slotsToAdvance >= subWindowCount) {
                    long[] arr = new long[subWindowCount + 1];
                    arr[subWindowCount] = currentSlot;
                    return arr;
                }
                // Clear advanced slots
                for (long s = 1; s <= slotsToAdvance; s++) {
                    int idx = (int) ((lastSlot + s) % subWindowCount);
                    v[idx] = 0;
                }
                v[subWindowCount] = currentSlot;
                return v;
            });

            // Sum all counters
            int total = 0;
            for (int i = 0; i < subWindowCount; i++) {
                total += counters[i];
            }

            if (total >= maxRequests) {
                long elapsed = now % windowMs;
                long retryAfterMs = windowMs - elapsed;
                long retryAfterSeconds = Math.max(1, (retryAfterMs + 999) / 1000);
                return new AcquireResult(false, 0, retryAfterSeconds);
            }

            counters[(int) currentSlot]++;
            return new AcquireResult(true, maxRequests - total - 1, 0);
        }

        /**
         * 清理过期的窗口条目
         *
         * @return 清理的条目数量
         */
        int cleanup() {
            long now = System.currentTimeMillis();
            long subWindowMs = windowMs / subWindowCount;
            long currentSlot = (now / subWindowMs) % subWindowCount;
            int cleaned = 0;
            Iterator<Map.Entry<String, long[]>> it = windows.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, long[]> entry = it.next();
                long lastSlot = entry.getValue()[subWindowCount];
                long age = (currentSlot - lastSlot + subWindowCount) % subWindowCount;
                if (age >= subWindowCount) {
                    it.remove();
                    cleaned++;
                }
            }
            return cleaned;
        }
    }
}
