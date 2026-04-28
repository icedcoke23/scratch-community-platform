package com.scratch.community.common.config;

import com.scratch.community.common.auth.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket + STOMP 配置
 *
 * 协议说明:
 * - 前端通过 SockJS/STOMP 连接 /ws-collab 端点
 * - 客户端发送消息到 /app/collab/* (前缀)
 * - 服务端推送消息到 /topic/collab/{sessionId}/* (广播)
 * - 服务端推送消息到 /user/queue/collab/* (点对点)
 *
 * 安全说明:
 * - WebSocket 握手时从 Authorization header 提取 JWT
 * - 解析出 userId 设置为 session attribute
 * - STOMP 消息拦截器从 header 中恢复 userId 到 LoginUser ThreadLocal
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(createTaskScheduler())
                .setHeartbeatValue(new long[]{10000, 10000});
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-collab")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new JwtHandshakeHandler())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtChannelInterceptor());
    }

    /**
     * 创建 WebSocket 消息调度器
     * 使用 ThreadPoolTaskScheduler 替代 DefaultManagedTaskScheduler
     * 后者需要 JNDI 管理的 ScheduledExecutor，在独立 Spring Boot 应用中不可用
     */
    private org.springframework.scheduling.TaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-broker-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * WebSocket 握手处理器：从请求中提取 JWT，创建带 userId 的 Principal
     */
    private class JwtHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                          Map<String, Object> attributes) {
            String token = extractToken(request);
            if (token != null) {
                try {
                    var claims = jwtUtils.parseToken(token);
                    String userId = claims.getSubject();
                    String username = claims.get("username", String.class);
                    // 存储到 session attributes，后续拦截器会读取
                    attributes.put("userId", userId);
                    attributes.put("username", username);
                    // 返回 Principal，支持 /user/ 目标前缀
                    return () -> userId;
                } catch (Exception e) {
                    log.warn("WebSocket JWT 解析失败: {}", e.getMessage());
                }
            }
            return () -> "anonymous";
        }

        private String extractToken(ServerHttpRequest request) {
            // 从 query 参数获取（SockJS 不支持自定义 header）
            String query = request.getURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        return param.substring(6);
                    }
                }
            }
            // 从 Authorization header 获取（原生 WebSocket）
            if (request instanceof ServletServerHttpRequest servletRequest) {
                HttpServletRequest sr = servletRequest.getServletRequest();
                String auth = sr.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    return auth.substring(7);
                }
            }
            return null;
        }
    }

    /**
     * STOMP 消息拦截器：从 header/session 中恢复 userId 到 LoginUser
     */
    private class JwtChannelInterceptor implements ChannelInterceptor {
        @Override
        public org.springframework.messaging.Message<?> preSend(
                org.springframework.messaging.Message<?> message,
                org.springframework.messaging.MessageChannel channel) {

            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                // CONNECT 帧：从 session attributes 读取 userId
                Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                if (sessionAttrs != null && sessionAttrs.containsKey("userId")) {
                    String userId = (String) sessionAttrs.get("userId");
                    String username = (String) sessionAttrs.get("username");
                    accessor.setUser(() -> userId);
                    log.debug("WebSocket 用户连接: userId={}, username={}", userId, username);
                }
            }
            return message;
        }
    }
}
