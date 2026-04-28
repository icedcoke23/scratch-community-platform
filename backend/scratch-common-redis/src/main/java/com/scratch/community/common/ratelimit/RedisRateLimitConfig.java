package com.scratch.community.common.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Redis 分布式限流配置
 *
 * <p>当 Redis 可用时自动启用分布式限流，替代内存限流器。
 * Redis 不可用时降级为不限流（由内存限流器兜底）。
 */
@Configuration
@ConditionalOnBean(RedisRateLimitInterceptor.class)
public class RedisRateLimitConfig implements WebMvcConfigurer {

    @Autowired
    private RedisRateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(1); // 在 AuthInterceptor 之后执行
    }
}
