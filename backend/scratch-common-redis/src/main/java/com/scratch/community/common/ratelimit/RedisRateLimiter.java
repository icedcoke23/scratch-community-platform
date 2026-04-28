package com.scratch.community.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Redis 分布式限流器（滑动窗口 + Lua 原子操作）
 *
 * <p>替代原有的内存限流器（ConcurrentHashMap），支持多实例部署。
 * 使用 Redis Lua 脚本保证原子性，避免并发竞态。
 *
 * <p>限流策略: 滑动窗口计数器
 * - 窗口大小: 可配置（默认 60 秒）
 * - 限流阈值: 可配置（如 60 次/分钟）
 * - 返回剩余次数和重置时间
 *
 * <p>性能: 单次限流检查 ~1ms（Redis 网络往返）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * 滑动窗口限流 Lua 脚本
     *
     * KEYS[1] = 限流 key
     * ARGV[1] = 窗口大小（秒）
     * ARGV[2] = 最大请求数
     * ARGV[3] = 当前时间戳（毫秒）
     *
     * 返回: {allowed(0/1), remaining, retryAfterMs}
     */
    private static final String SLIDING_WINDOW_SCRIPT = """
            local key = KEYS[1]
            local window_ms = tonumber(ARGV[1]) * 1000
            local max_requests = tonumber(ARGV[2])
            local now_ms = tonumber(ARGV[3])
            local window_start = now_ms - window_ms
            
            -- 移除窗口外的旧记录
            redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)
            
            -- 当前窗口内的请求数
            local current = redis.call('ZCARD', key)
            
            if current < max_requests then
                -- 未超限，添加当前请求
                redis.call('ZADD', key, now_ms, now_ms .. '-' .. math.random())
                redis.call('PEXPIRE', key, window_ms)
                return {1, max_requests - current - 1, 0}
            else
                -- 已超限，计算重试时间
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                local retry_after = 0
                if #oldest > 0 then
                    retry_after = tonumber(oldest[2]) + window_ms - now_ms
                end
                return {0, 0, math.max(0, retry_after)}
            end
            """;

    /**
     * 使用 List 接收 Lua 脚本的多值返回（fix: 坑 — Lua 返回值类型不匹配）
     *
     * <p>Lua 脚本返回 {allowed, remaining, retryAfter} 三个值的 table，
     * Spring RedisTemplate 的 {@code DefaultRedisScript<List>} 可以正确接收。
     * 之前使用 {@code DefaultRedisScript<Long>} 只能取到第一个值，丢失了 remaining 和 retryAfter。
     */
    private final DefaultRedisScript<List> rateLimitScript = createScript();

    @SuppressWarnings("unchecked")
    private static DefaultRedisScript<List> createScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(SLIDING_WINDOW_SCRIPT);
        script.setResultType(List.class);
        return script;
    }

    /**
     * 检查是否允许请求
     *
     * @param key          限流 key（如 "rate:user:123" 或 "rate:ip:1.2.3.4"）
     * @param windowSeconds 窗口大小（秒）
     * @param maxRequests  窗口内最大请求数
     * @return 限流结果
     */
    @SuppressWarnings("unchecked")
    public RateLimitResult tryAcquire(String key, int windowSeconds, int maxRequests) {
        try {
            // 使用 EVALSHA 执行 Lua 脚本（原子操作）
            List<Long> result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(windowSeconds),
                    String.valueOf(maxRequests),
                    String.valueOf(System.currentTimeMillis())
            );

            if (result == null || result.size() < 3) {
                // Redis 异常，降级放行
                log.warn("Redis 限流脚本执行失败，降级放行: key={}", key);
                return RateLimitResult.allowed(maxRequests - 1, 0);
            }

            // Lua 脚本返回 {allowed(0/1), remaining, retryAfterMs}
            boolean allowed = result.get(0) == 1L;
            long remaining = result.get(1);
            long retryAfterMs = result.get(2);

            if (allowed) {
                return RateLimitResult.allowed(remaining, 0);
            } else {
                return RateLimitResult.rejected(0, retryAfterMs);
            }

        } catch (Exception e) {
            // Redis 不可用时降级放行
            log.warn("Redis 限流异常，降级放行: key={}, error={}", key, e.getMessage());
            return RateLimitResult.allowed(maxRequests - 1, 0);
        }
    }

    /**
     * 简化版限流（使用 INCR + EXPIRE，适用于固定窗口）
     *
     * <p>注意: 使用 Lua 脚本保证 INCR + EXPIRE 的原子性，
     * 避免 INCR 成功但 EXPIRE 失败（进程崩溃）导致 key 永不过期。
     *
     * @param key           限流 key
     * @param windowSeconds 窗口大小（秒）
     * @param maxRequests   最大请求数
     * @return 是否允许
     */
    public boolean isAllowed(String key, int windowSeconds, int maxRequests) {
        try {
            // Lua 脚本保证 INCR + EXPIRE 原子执行
            Long count = redisTemplate.execute(
                    FIXED_WINDOW_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(windowSeconds),
                    String.valueOf(maxRequests)
            );
            return count != null && count <= maxRequests;
        } catch (Exception e) {
            log.warn("Redis 限流异常，降级放行: key={}", key);
            return true; // 降级放行
        }
    }

    /** 固定窗口限流 Lua 脚本（原子 INCR + EXPIRE） */
    private static final DefaultRedisScript<Long> FIXED_WINDOW_SCRIPT = createFixedWindowScript();

    private static DefaultRedisScript<Long> createFixedWindowScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
        """);
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 获取当前窗口的剩余次数
     */
    public long getRemaining(String key, int maxRequests) {
        try {
            String val = redisTemplate.opsForValue().get(key);
            long used = val != null ? Long.parseLong(val) : 0;
            return Math.max(0, maxRequests - used);
        } catch (Exception e) {
            return maxRequests;
        }
    }

    /**
     * 重置限流计数
     */
    public void reset(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("重置限流失败: key={}", key);
        }
    }

    /**
     * 限流结果
     */
    public record RateLimitResult(boolean allowed, long remaining, long retryAfterMs) {

        public static RateLimitResult allowed(long remaining, long retryAfterMs) {
            return new RateLimitResult(true, remaining, retryAfterMs);
        }

        public static RateLimitResult rejected(long remaining, long retryAfterMs) {
            return new RateLimitResult(false, remaining, retryAfterMs);
        }
    }
}
