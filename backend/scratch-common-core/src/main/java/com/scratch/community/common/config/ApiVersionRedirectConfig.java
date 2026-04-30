package com.scratch.community.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 版本向后兼容重定向
 *
 * <p>将旧版 /api/xxx 请求重定向到 /api/v1/xxx，保持向后兼容。
 * 使用 307 临时重定向，保留原始 HTTP 方法和请求体。
 *
 * <p>排除已经带版本号的路径（/api/v1/**）和健康检查（/api/health）。
 *
 * @author scratch-community
 */
@Slf4j
@Configuration
public class ApiVersionRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String uri = request.getRequestURI();

                // 安全检查：拒绝包含路径遍历的 URI（防止 /api/../ 攻击）
                if (uri.contains("..")) {
                    log.warn("拒绝可疑的路径遍历请求: {}", uri);
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return false;
                }

                // 只处理 /api/ 开头且不以 /api/v1/ 开头的路径
                if (uri.startsWith("/api/") && !uri.startsWith("/api/v1/")) {
                    String newUri = "/api/v1" + uri.substring(4); // "/api/" → "/api/v1/"
                    String query = request.getQueryString();
                    if (query != null) {
                        newUri = newUri + "?" + query;
                    }
                    log.debug("API 版本重定向: {} → {}", uri, newUri);
                    response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
                    response.setHeader("Location", newUri);
                    return false;
                }
                return true;
            }
        })
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/v1/**", "/api/health");
    }
}
