package com.scratch.community.common.config;

import java.lang.annotation.*;

/**
 * 接口限流注解
 *
 * <p>标注在 Controller 方法上，为单个接口配置独立的限流策略。
 * 未标注此注解的接口继续使用 {@link RateLimitConfig} 中的全局拦截器限流。
 *
 * <p>使用示例:
 * <pre>
 * // 10 次/分钟
 * {@code @RateLimit(maxRequests = 10, windowSeconds = 60)}
 * {@code @PostMapping("/user/register")}
 * public R&lt;?&gt; register(...) { ... }
 *
 * // 5 次/秒（高频接口）
 * {@code @RateLimit(maxRequests = 5, windowSeconds = 1)}
 * {@code @GetMapping("/feed")}
 * public R&lt;?&gt; getFeed(...) { ... }
 * </pre>
 *
 * <p>注意: 此注解的限流基于客户端 IP，使用与 {@link RateLimitConfig} 相同的滑动窗口计数器算法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 窗口内最大请求数
     */
    int maxRequests() default 60;

    /**
     * 窗口时长（秒）
     */
    int windowSeconds() default 60;

    /**
     * 限流 key 的前缀（可选，默认使用请求路径）
     * <p>用于区分同一接口的不同限流维度（如按用户 ID 限流）
     */
    String keyPrefix() default "";
}
