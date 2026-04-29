package com.scratch.community.api;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

/**
 * 测试环境 Redis Mock 配置
 *
 * <p>集成测试使用真实 MySQL，但 Redis 使用 mock（避免依赖真实 Redis 实例）。
 * 为所有 key-value 操作提供安全的默认返回值，防止 NPE 和 StackOverflow。
 *
 * <p>关键 stub：
 * - hasKey(key) → false（Token 未被黑名单）
 * - opsForValue().get(key) → null（Key 不存在）
 * - opsForValue().setIfAbsent(key, value) → true（首次请求成功获取锁）
 */
@TestConfiguration
public class TestRedisMockConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建 mock 实例
        StringRedisTemplate mock = mock(StringRedisTemplate.class);

        // 黑名单检查：默认 false
        when(mock.hasKey(anyString())).thenReturn(false);

        // opsForValue() 链：防止 NPE
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(mock.opsForValue()).thenReturn(valueOps);

        // get → null（Key 不存在）
        when(valueOps.get(anyString())).thenReturn(null);

        // setIfAbsent → true（幂等锁获取成功）
        when(valueOps.setIfAbsent(anyString(), anyString())).thenReturn(true);

        return mock;
    }

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        return mock(RedissonClient.class);
    }
}
