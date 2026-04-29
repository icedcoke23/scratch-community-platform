package com.scratch.community.api;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

/**
 * 测试环境 Redis Mock 配置
 *
 * <p>集成测试使用真实 MySQL，但 Redis 使用 mock（避免依赖真实 Redis 实例）。
 * 支持动态黑名单：logout 时调用 tokenBlacklistService.blacklist() 会记录 token，
 * 后续请求通过 TokenBlacklistService.isUserBlacklisted() 检查。
 */
@TestConfiguration
public class TestRedisMockConfig {

    /** 动态记录被黑名单的 token key */
    private static final Set<String> BLACKLIST_KEYS = ConcurrentHashMap.newKeySet();

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

        // 黑名单检查：动态检查 BLACKLIST_KEYS
        when(mock.hasKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return BLACKLIST_KEYS.contains(key);
        });

        // opsForValue() 链
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(mock.opsForValue()).thenReturn(valueOps);

        // get → null（Key 不存在）
        when(valueOps.get(anyString())).thenReturn(null);

        // setIfAbsent → true（首次获取锁成功）
        when(valueOps.setIfAbsent(anyString(), anyString())).thenReturn(true);

        // set(key, value, timeout, unit) → 记录到黑名单
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            BLACKLIST_KEYS.add(key);
            return null;
        }).when(valueOps).set(anyString(), anyString(), anyLong(), any(java.util.concurrent.TimeUnit.class));

        // set(key, value) → 普通记录
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            BLACKLIST_KEYS.add(key);
            return null;
        }).when(valueOps).set(anyString(), anyString());

        // set(key, value, options) → 三参数版本
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            BLACKLIST_KEYS.add(key);
            return null;
        }).when(valueOps).set(anyString(), anyString(), any());

        // ZSetOperations 用于排行榜（RankService）
        @SuppressWarnings("unchecked")
        org.springframework.data.redis.core.ZSetOperations<String, String> zSetOps = mock(org.springframework.data.redis.core.ZSetOperations.class);
        when(mock.opsForZSet()).thenReturn(zSetOps);
        // incrementScore 返回 Double 类型
        when(zSetOps.incrementScore(anyString(), anyString(), anyDouble())).thenReturn(0.0);
        // rangeWithScores 返回空 Set
        when(zSetOps.rangeWithScores(anyString(), anyInt(), anyInt()))
                .thenReturn(java.util.Collections.emptySet());

        return mock;
    }

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        return mock(RedissonClient.class);
    }
}
