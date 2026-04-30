package com.scratch.community.common.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * JWT Token 黑名单服务
 *
 * <p>使用 Redis 存储已失效的 Token，实现登出和 Token 失效功能。
 * <p>Token 黑名单的 TTL 与 Token 剩余有效期一致，过期后自动清理。
 *
 * <p>使用场景:
 * <ul>
 *   <li>用户主动登出 — Token 加入黑名单</li>
 *   <li>管理员禁用用户 — 该用户所有 Token 加入黑名单</li>
 *   <li>修改密码 — 旧 Token 加入黑名单</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKEN_PREFIX = "jwt:user:";

    /**
     * 将 Token 加入黑名单
     *
     * @param token      JWT Token
     * @param expireMs   Token 剩余有效期（毫秒）
     */
    public void blacklist(String token, long expireMs) {
        if (token == null || token.isBlank() || expireMs <= 0) {
            return;
        }
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", expireMs, TimeUnit.MILLISECONDS);
        log.debug("Token 已加入黑名单，剩余有效期: {}ms", expireMs);
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 将用户的所有 Token 加入黑名单（通过 userId）
     * <p>注意：这不会立即失效已签发的 Token，但 AuthInterceptor 会检查黑名单。
     * <p>实际效果取决于 Token 检查频率和 Redis 过期时间。
     *
     * @param userId   用户 ID
     * @param expireMs 最大过期时间（毫秒，通常等于 Token 最大有效期）
     */
    public void blacklistUser(Long userId, long expireMs) {
        if (userId == null || expireMs <= 0) {
            return;
        }
        String key = USER_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "disabled", expireMs, TimeUnit.MILLISECONDS);
        log.info("用户 Token 已标记失效: userId={}, expireMs={}", userId, expireMs);
    }

    /**
     * 检查用户是否被标记为 Token 失效
     */
    public boolean isUserBlacklisted(Long userId) {
        if (userId == null) {
            return false;
        }
        String key = USER_TOKEN_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
