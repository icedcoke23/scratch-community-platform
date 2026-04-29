package com.scratch.community.api;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.*;

/**
 * 测试环境 Redis Mock 配置
 * <p>
 * 集成测试使用真实 MySQL，但 Redis 使用 mock（避免依赖真实 Redis 实例）。
 * 为 key-value 操作提供默认返回值，防止 NPE。
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
        // 使用 RETURNS_SMART_NULLS 自动为方法调用返回智能 null（避免 NPE）
        StringRedisTemplate mock = mock(StringRedisTemplate.class, RETURNS_SMART_NULLS);
        // 黑名单检查默认返回 false（Token 未被加入黑名单）
        when(mock.hasKey(anyString())).thenReturn(false);
        return mock;
    }

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        return mock(RedissonClient.class);
    }
}
