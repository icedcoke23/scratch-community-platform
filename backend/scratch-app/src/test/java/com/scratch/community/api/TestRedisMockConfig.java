package com.scratch.community.api;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * 测试环境 Redis Mock 配置
 * <p>
 * 集成测试不需要真实 Redis，通过 MockBean 替代：
 * - StringRedisTemplate: 替代 Spring Boot 自动配置的 Redis 模板
 * - RedissonClient: 替代分布式锁客户端
 * - RedisConnectionFactory: 阻止 Spring Boot 尝试连接 Redis
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
        // 使用 Mockito mock，所有 Redis 操作返回 null/默认值
        // 这样 TokenBlacklistService、SseTokenService、IdempotentInterceptor 等不会 NPE
        return mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        // Mock RedissonClient，PointService 会降级为数据库原子 SQL
        return mock(RedissonClient.class);
    }
}
