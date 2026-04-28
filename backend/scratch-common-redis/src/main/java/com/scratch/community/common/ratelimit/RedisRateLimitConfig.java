package com.scratch.community.common.ratelimit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Redis 分布式限流配置
 *
 * <p>注册 Redis 限流拦截器到 /api/** 路径
 */
@Configuration
public class RedisRateLimitConfig implements WebMvcConfigurer {

    private final RedisRateLimitInterceptor rateLimitInterceptor;

    public RedisRateLimitConfig(RedisRateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(1);
    }
}
