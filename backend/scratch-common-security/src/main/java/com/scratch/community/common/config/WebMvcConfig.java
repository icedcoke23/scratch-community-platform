package com.scratch.community.common.config;

import com.scratch.community.common.auth.AuthInterceptor;
import com.scratch.community.common.auth.JwtUtils;
import com.scratch.community.common.auth.SseTokenService;
import com.scratch.community.common.auth.TokenBlacklistService;
import com.scratch.community.common.idempotent.IdempotentInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 - CORS + 拦截器 + API 版本重定向
 *
 * <p>拦截器注册：
 * <ul>
 *   <li>AuthInterceptor - JWT 认证（排除公开接口）</li>
 *   <li>IdempotentInterceptor - 幂等性保护</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtils jwtUtils;
    private final SseTokenService sseTokenService;
    private final ObjectMapper objectMapper;
    private final IdempotentInterceptor idempotentInterceptor;

    /** CORS 允许的源（逗号分隔），默认允许 localhost */
    @Value("${cors.allowed-origins:http://localhost:*,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
                .exposedHeaders("X-Total-Count", "X-Page-Count")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 认证拦截器（手动创建实例，确保排除路径生效）
        AuthInterceptor authInterceptor = new AuthInterceptor(
                jwtUtils,
                tokenBlacklistService,
                sseTokenService,
                objectMapper
        );
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**", "/api/v1/**")
                .excludePathPatterns(
                        // 用户模块 - 公开接口
                        "/api/user/register",
                        "/api/user/login",
                        "/api/v1/user/register",
                        "/api/v1/user/login",
                        // Refresh Token 由 controller 内部用 REFRESH_SECRET 验证
                        "/api/v1/user/refresh",

                        // 社区模块 - 未登录可浏览
                        "/api/social/feed",
                        "/api/social/search",
                        "/api/social/rank/**",
                        "/api/social/project/*/comments",
                        "/api/v1/social/feed",
                        "/api/v1/social/search",
                        "/api/v1/social/rank/**",
                        "/api/v1/social/project/*/comments",

                        // 题目模块 - 未登录可浏览
                        "/api/problem",
                        "/api/problem/{id}",
                        "/api/v1/problem",
                        "/api/v1/problem/{id}",

                        // 作业模块 - 未登录可浏览
                        "/api/homework/class/**",
                        "/api/homework/{id}",
                        "/api/v1/homework/class/**",
                        "/api/v1/homework/{id}",

                        // 竞赛模块 - 未登录可浏览
                        "/api/competition",
                        "/api/competition/{id}",
                        "/api/v1/competition",
                        "/api/v1/competition/{id}",

                        // 健康检查
                        "/api/health",
                        "/api/v1/health",

                        // 平台统计（公开）
                        "/api/v1/stats",

                        // Swagger
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );

        // 幂等性拦截器（仅拦截写接口）
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/api/**", "/api/v1/**");
    }
}
