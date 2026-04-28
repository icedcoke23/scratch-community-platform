package com.scratch.community.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证 + 角色校验拦截器
 *
 * <p>安全改进:
 * <ul>
 *   <li>Token 黑名单检查 — 支持登出和 Token 失效</li>
 *   <li>用户级黑名单检查 — 支持管理员禁用用户</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final SseTokenService sseTokenService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 非控制器方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String uri = request.getRequestURI();

        // SSE 一次性 Token 认证（优先级最高，仅限 /stream 端点）
        LoginUser loginUser = null;
        if (uri.endsWith("/stream")) {
            String sseToken = request.getParameter("sse_token");
            if (sseToken != null && !sseToken.isBlank()) {
                Long userId = sseTokenService.consumeToken(sseToken);
                if (userId != null) {
                    loginUser = new LoginUser();
                    loginUser.setUserId(userId);
                    // SSE Token 只能验证 userId，角色信息需要从 JWT 获取或使用默认值
                    // 尝试从 Authorization Header 补充角色信息
                    String bearerToken = request.getHeader("Authorization");
                    if (bearerToken != null && bearerToken.startsWith("Bearer ") && jwtUtils.validateToken(bearerToken.substring(7))) {
                        loginUser.setUsername(jwtUtils.getUsername(bearerToken.substring(7)));
                        loginUser.setRole(jwtUtils.getRole(bearerToken.substring(7)));
                    } else {
                        loginUser.setUsername("sse-user");
                        loginUser.setRole("STUDENT");
                    }
                }
            }
        }

        // 非 SSE Token 认证：走标准 JWT 流程
        if (loginUser == null) {
            String token = extractToken(request);
            if (token == null || !jwtUtils.validateToken(token)) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                return false;
            }

            // 检查 Token 黑名单（登出/失效）
            if (tokenBlacklistService.isBlacklisted(token)) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.TOKEN_EXPIRED);
                return false;
            }

            // 解析用户信息并设置到上下文
            loginUser = new LoginUser();
            loginUser.setUserId(jwtUtils.getUserId(token));
            loginUser.setUsername(jwtUtils.getUsername(token));
            loginUser.setRole(jwtUtils.getRole(token));

            // 检查用户级黑名单（管理员禁用）
            if (tokenBlacklistService.isUserBlacklisted(loginUser.getUserId())) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.TOKEN_EXPIRED);
                return false;
            }
        }

        LoginUser.set(loginUser);

        // 角色校验（使用 Role 枚举安全解析，避免 typo 风险）
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null) {
            Set<Role> allowedRoles = Arrays.stream(requireRole.value())
                    .map(Role::fromString)
                    .collect(Collectors.toSet());
            Role userRole = Role.fromString(loginUser.getRole());
            if (!allowedRoles.contains(userRole)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN);
                return false;
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUser.remove();
    }

    /**
     * 提取 JWT Token
     *
     * <p>支持方式:
     * 1. Authorization: Bearer xxx（标准方式）
     *
     * <p>注意: SSE 端点使用一次性 Token（sse_token 参数），在 preHandle 中单独处理。
     */
    private String extractToken(HttpServletRequest request) {
        // Authorization Header
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void writeError(HttpServletResponse response, int httpStatus, ErrorCode errorCode) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(errorCode)));
    }
}
