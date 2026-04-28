package com.scratch.community.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 请求日志配置
 *
 * 记录每个 API 请求的方法、路径、耗时、状态码，便于生产环境排查问题。
 * 静态资源和健康检查接口不记录。
 */
@Slf4j
@Configuration
public class RequestLoggingConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestLoggingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health")
                .order(0); // 最高优先级，在限流之前
    }

    /**
     * 请求日志拦截器
     */
    static class RequestLoggingInterceptor implements HandlerInterceptor {

        private static final String START_TIME_ATTR = "requestStartTime";

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            if (startTime == null) return;

            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String query = sanitizeQueryString(request.getQueryString());
            String ip = getClientIp(request);

            // 慢请求阈值：1000ms
            if (duration > 1000) {
                log.warn("SLOW {} {} {} {}ms ip={}", status, method,
                        query != null ? uri + "?" + query : uri, duration, ip);
            } else if (status >= 400) {
                log.warn("FAIL {} {} {} {}ms ip={}", status, method,
                        query != null ? uri + "?" + query : uri, duration, ip);
            } else {
                log.info("REQ  {} {} {} {}ms", status, method,
                        query != null ? uri + "?" + query : uri, duration);
            }
        }

        /**
         * 脱敏 Query String 中的敏感参数
         */
        private String sanitizeQueryString(String query) {
            if (query == null || query.isBlank()) return query;
            // 脱敏 password、token、secret 等敏感参数
            return query.replaceAll("(?i)(password|token|secret|key|authorization)=[^&]*", "$1=***");
        }

        /**
         * 获取客户端 IP（与 RateLimitConfig 保持一致的安全策略）
         */
        private String getClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Real-IP");
            if (ip != null && !ip.isBlank()) {
                return ip.trim();
            }
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
            return request.getRemoteAddr();
        }
    }
}
