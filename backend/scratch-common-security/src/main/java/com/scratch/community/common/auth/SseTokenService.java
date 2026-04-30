package com.scratch.community.common.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE 一次性 Token 服务
 *
 * <p>解决 SSE（Server-Sent Events）流式端点通过 URL query 参数传递 JWT Token 的安全风险：
 * JWT Token 可能出现在服务器访问日志、代理日志、浏览器历史等地方。
 *
 * <p>方案：
 * <ol>
 *   <li>客户端先调用 REST 接口获取一次性 Token（绑定 userId，TTL 5 分钟）</li>
 *   <li>客户端用一次性 Token 建立 SSE 连接</li>
 *   <li>服务端验证并消费 Token（一次性删除），从中获取 userId</li>
 * </ol>
 *
 * <p>降级策略：Redis 不可用时，自动降级为内存 Map（带过期清理）。
 *
 * @author scratch-community
 */
@Slf4j
@Service
public class SseTokenService {

    private static final String REDIS_KEY_PREFIX = "sse:token:";
    private static final long TOKEN_TTL_MINUTES = 5;
    private static final int TOKEN_LENGTH = 32;

    private final StringRedisTemplate redisTemplate;
    private final boolean redisAvailable;

    /** 降级方案：内存存储 */
    private final Map<String, TokenEntry> memoryStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-token-cleaner");
        t.setDaemon(true);
        return t;
    });

    private static final SecureRandom RANDOM = new SecureRandom();

    public SseTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisAvailable = checkRedis();
        if (!redisAvailable) {
            log.warn("Redis 不可用，SSE Token 降级为内存存储");
        }
        // 无论 Redis 是否可用，都启动清理任务
        // 当 Redis 断连降级到内存时，清理器已在运行
        cleaner.scheduleAtFixedRate(this::cleanExpiredTokens, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 生成一次性 SSE Token
     *
     * @param userId 用户 ID
     * @return 生成的一次性 Token（Base64url 编码）
     */
    public String generateToken(Long userId) {
        String token = generateRandomToken();
        String value = String.valueOf(userId);

        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(
                        REDIS_KEY_PREFIX + token,
                        value,
                        TOKEN_TTL_MINUTES,
                        TimeUnit.MINUTES);
                log.debug("SSE Token 已生成（Redis）: userId={}", userId);
                return token;
            } catch (Exception e) {
                log.warn("Redis 存储 SSE Token 失败，降级到内存: {}", e.getMessage());
                // Redis 断连，标记为不可用，后续请求直接走内存
                // 注意：清理器已在构造函数中启动，无需额外操作
            }
        }

        // 降级：内存存储
        long expireAt = System.currentTimeMillis() + TOKEN_TTL_MINUTES * 60 * 1000;
        memoryStore.put(token, new TokenEntry(userId, expireAt));
        log.debug("SSE Token 已生成（内存）: userId={}", userId);
        return token;
    }

    /**
     * 验证并消费一次性 Token（原子操作）
     *
     * <p>成功消费后 Token 立即失效，不可重复使用。
     *
     * @param token 一次性 Token
     * @return 关联的 userId，Token 无效或已过期则返回 null
     */
    public Long consumeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        if (redisAvailable) {
            try {
                String key = REDIS_KEY_PREFIX + token;
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    // 原子删除（一次性）
                    redisTemplate.delete(key);
                    log.debug("SSE Token 已消费（Redis）: userId={}", value);
                    return Long.parseLong(value);
                }
                return null;
            } catch (Exception e) {
                log.warn("Redis 消费 SSE Token 失败，降级到内存: {}", e.getMessage());
            }
        }

        // 降级：内存存储
        TokenEntry entry = memoryStore.remove(token);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            return null; // 已过期
        }
        log.debug("SSE Token 已消费（内存）: userId={}", entry.userId);
        return entry.userId;
    }

    // ==================== 私有方法 ====================

    private String generateRandomToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean checkRedis() {
        try {
            redisTemplate.hasKey("sse:token:health-check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        int cleaned = 0;
        var it = memoryStore.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAt < now) {
                it.remove();
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.debug("清理过期 SSE Token: {} 个", cleaned);
        }
    }

    /**
     * 内存 Token 条目
     */
    private record TokenEntry(Long userId, long expireAt) {}
}
