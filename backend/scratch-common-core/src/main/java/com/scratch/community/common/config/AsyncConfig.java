package com.scratch.community.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


/**
 * 异步任务配置
 * 用于判题等耗时操作的非阻塞执行
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 判题专用线程池
     * 核心线程 4，最大线程 16，队列容量 100
     */
    @Bean("judgeExecutor")
    public ThreadPoolTaskExecutor judgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("judge-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.error("判题线程池已满，任务被拒绝");
            throw new RuntimeException("判题服务繁忙，请稍后重试");
        });
        executor.initialize();
        log.info("判题线程池初始化完成: core=4, max=16, queue=100");
        return executor;
    }

    /**
     * SSE 流式任务线程池
     * 核心线程 2，最大线程 8，队列容量 50
     */
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sse-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.error("SSE 线程池已满，任务被拒绝");
            throw new RuntimeException("服务繁忙，请稍后重试");
        });
        executor.initialize();
        log.info("SSE 线程池初始化完成: core=2, max=8, queue=50");
        return executor;
    }

    /**
     * 事件监听专用线程池
     * 用于 Spring Event 异步监听器（点赞/评论/浏览等事件的异步处理）
     * 与 SSE 和判题线程池隔离，避免互相影响
     */
    @Bean("eventExecutor")
    public ThreadPoolTaskExecutor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("event-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("事件监听线程池已满，任务被拒绝");
            // 事件监听失败不应影响主业务，记录日志即可
        });
        executor.initialize();
        log.info("事件监听线程池初始化完成: core=2, max=8, queue=200");
        return executor;
    }
}
