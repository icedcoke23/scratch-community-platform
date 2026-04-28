package com.scratch.community.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器
 *
 * <p>为每个请求生成唯一的 TraceId，写入 MDC（Mapped Diagnostic Context），
 * 所有日志自动携带 TraceId，便于问题排查和链路追踪。
 *
 * <p>特性:
 * <ul>
 *   <li>自动生成 UUID 格式的 TraceId</li>
 *   <li>支持客户端通过 {@code X-Trace-Id} 请求头传入（分布式场景）</li>
 *   <li>响应头中返回 TraceId，便于前端关联</li>
 *   <li>请求结束后自动清理 MDC，防止线程复用导致的日志污染</li>
 * </ul>
 *
 * <p>日志格式建议:
 * <pre>
 * %d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n
 * </pre>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
