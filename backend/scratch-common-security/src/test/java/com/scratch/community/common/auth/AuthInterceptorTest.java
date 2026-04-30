package com.scratch.community.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AuthInterceptor 单元测试
 * 重点测试 SSE 端点的 Token 提取逻辑
 */
class AuthInterceptorTest {

    private final AuthInterceptor interceptor;
    private final JwtUtils jwtUtils;

    AuthInterceptorTest() {
        jwtUtils = new JwtUtils();
        setField(jwtUtils, "secret", "test-secret-key-at-least-32-bytes-long!!!");
        setField(jwtUtils, "expiration", 86400000L);
        jwtUtils.validateConfig();

        TokenBlacklistService blacklistService = new TokenBlacklistService(mock(StringRedisTemplate.class));
        SseTokenService sseTokenService = new SseTokenService(mock(StringRedisTemplate.class));
        interceptor = new AuthInterceptor(jwtUtils, blacklistService, sseTokenService, new ObjectMapper());
    }

    @Test
    @DisplayName("从 Authorization Header 提取 Token")
    void extractToken_fromHeader() throws Exception {
        String token = jwtUtils.generateToken(1L, "user", "STUDENT");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        String extracted = invokeExtractToken(request);
        assertEquals(token, extracted);
    }

    @Test
    @DisplayName("extractToken 不从 query 参数提取（SSE 已改用 SseTokenService）")
    void extractToken_noQueryExtraction() throws Exception {
        String token = jwtUtils.generateToken(1L, "user", "STUDENT");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/ai-review/project/1/stream");
        request.setParameter("token", token);

        String extracted = invokeExtractToken(request);
        assertNull(extracted);
    }

    @Test
    @DisplayName("非 SSE 端点不从 query 参数提取 Token")
    void extractToken_noQueryForNonSSE() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/user/me");
        request.setParameter("token", "some-token");

        String extracted = invokeExtractToken(request);
        assertNull(extracted);
    }

    @Test
    @DisplayName("Header 优先于 query 参数")
    void extractToken_headerPriority() throws Exception {
        String headerToken = jwtUtils.generateToken(1L, "user", "STUDENT");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/ai-review/project/1/stream");
        request.addHeader("Authorization", "Bearer " + headerToken);
        request.setParameter("token", "query-token");

        String extracted = invokeExtractToken(request);
        assertEquals(headerToken, extracted);
    }

    @Test
    @DisplayName("无 Token 返回 null")
    void extractToken_noToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertNull(invokeExtractToken(request));
    }

    // 反射调用私有方法
    private String invokeExtractToken(MockHttpServletRequest request) throws Exception {
        Method method = AuthInterceptor.class.getDeclaredMethod("extractToken",
                jakarta.servlet.http.HttpServletRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(interceptor, request);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
