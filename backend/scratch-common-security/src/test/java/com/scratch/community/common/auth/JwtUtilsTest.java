package com.scratch.community.common.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtils 单元测试
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // 用反射设置私有字段（测试环境）
        setField(jwtUtils, "secret", "test-secret-key-at-least-32-bytes-long!!!");
        setField(jwtUtils, "expiration", 86400000L); // 24 小时
        jwtUtils.validateConfig();
    }

    @Nested
    @DisplayName("Token 生成与解析")
    class GenerateAndParseTests {

        @Test
        @DisplayName("生成 Token 不为空")
        void generateToken_notEmpty() {
            String token = jwtUtils.generateToken(1L, "testuser", "STUDENT");
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("解析 Token 获取 userId")
        void getUserId_fromToken() {
            String token = jwtUtils.generateToken(42L, "alice", "TEACHER");
            assertEquals(42L, jwtUtils.getUserId(token));
        }

        @Test
        @DisplayName("解析 Token 获取 username")
        void getUsername_fromToken() {
            String token = jwtUtils.generateToken(1L, "bob", "ADMIN");
            assertEquals("bob", jwtUtils.getUsername(token));
        }

        @Test
        @DisplayName("解析 Token 获取 role")
        void getRole_fromToken() {
            String token = jwtUtils.generateToken(1L, "user", "TEACHER");
            assertEquals("TEACHER", jwtUtils.getRole(token));
        }
    }

    @Nested
    @DisplayName("Token 验证")
    class ValidationTests {

        @Test
        @DisplayName("有效 Token 验证通过")
        void validateToken_valid() {
            String token = jwtUtils.generateToken(1L, "user", "STUDENT");
            assertTrue(jwtUtils.validateToken(token));
        }

        @Test
        @DisplayName("篡改 Token 验证失败")
        void validateToken_tampered() {
            String token = jwtUtils.generateToken(1L, "user", "STUDENT");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertFalse(jwtUtils.validateToken(tampered));
        }

        @Test
        @DisplayName("空 Token 验证失败")
        void validateToken_empty() {
            assertFalse(jwtUtils.validateToken(""));
            assertFalse(jwtUtils.validateToken(null));
        }

        @Test
        @DisplayName("随机字符串验证失败")
        void validateToken_random() {
            assertFalse(jwtUtils.validateToken("not.a.jwt.token"));
        }
    }

    @Nested
    @DisplayName("密钥校验")
    class ConfigValidationTests {

        @Test
        @DisplayName("密钥不足 32 字节抛异常")
        void validateConfig_shortKey() {
            JwtUtils shortKey = new JwtUtils();
            setField(shortKey, "secret", "short");
            setField(shortKey, "expiration", 86400000L);
            assertThrows(IllegalStateException.class, shortKey::validateConfig);
        }

        @Test
        @DisplayName("密钥为空且非 prod 环境使用默认值")
        void validateConfig_emptyKey_dev() {
            JwtUtils emptyKey = new JwtUtils();
            setField(emptyKey, "secret", "");
            setField(emptyKey, "expiration", 86400000L);
            // dev 环境不应抛异常（使用默认密钥）
            assertDoesNotThrow(emptyKey::validateConfig);
        }
    }

    // 反射工具方法
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
