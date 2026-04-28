package com.scratch.community.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程池监控配置
 *
 * <p>将 3 个业务线程池（判题/SSE/事件）的关键指标注册到 Micrometer，
 * 可通过 {@code GET /actuator/metrics/} 或 Prometheus 查询。
 *
 * <p>暴露的指标:
 * <ul>
 *   <li>{@code threadpool.active.{name}} — 活跃线程数</li>
 *   <li>{@code threadpool.pool.size.{name}} — 当前池大小</li>
 *   <li>{@code threadpool.queue.size.{name}} — 队列中等待的任务数</li>
 *   <li>{@code threadpool.completed.{name}} — 已完成任务总数</li>
 * </ul>
 *
 * <p>健康检查: 当任意线程池队列使用率 > 80% 时，Health 状态降为 WARNING。
 */
@Slf4j
@Configuration
public class ThreadPoolMonitorConfig implements HealthIndicator {

    private final Map<String, ThreadPoolTaskExecutor> executorMap = new ConcurrentHashMap<>();

    public ThreadPoolMonitorConfig(
            @Qualifier("judgeExecutor") ThreadPoolTaskExecutor judgeExecutor,
            @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
            @Qualifier("eventExecutor") ThreadPoolTaskExecutor eventExecutor,
            MeterRegistry meterRegistry) {

        executorMap.put("judge", judgeExecutor);
        executorMap.put("sse", taskExecutor);
        executorMap.put("event", eventExecutor);

        // 注册 Micrometer 指标
        for (Map.Entry<String, ThreadPoolTaskExecutor> entry : executorMap.entrySet()) {
            String name = entry.getKey();
            ThreadPoolTaskExecutor executor = entry.getValue();

            Gauge.builder("threadpool.active", executor, e -> e.getThreadPoolExecutor().getActiveCount())
                    .tag("pool", name)
                    .description("活跃线程数")
                    .register(meterRegistry);

            Gauge.builder("threadpool.pool.size", executor, e -> e.getThreadPoolExecutor().getPoolSize())
                    .tag("pool", name)
                    .description("当前池大小")
                    .register(meterRegistry);

            Gauge.builder("threadpool.queue.size", executor, e -> e.getThreadPoolExecutor().getQueue().size())
                    .tag("pool", name)
                    .description("队列中等待的任务数")
                    .register(meterRegistry);

            Gauge.builder("threadpool.completed", executor, e -> e.getThreadPoolExecutor().getCompletedTaskCount())
                    .tag("pool", name)
                    .description("已完成任务总数")
                    .register(meterRegistry);
        }

        log.info("线程池监控注册完成: {}", executorMap.keySet());
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        boolean degraded = false;

        for (Map.Entry<String, ThreadPoolTaskExecutor> entry : executorMap.entrySet()) {
            String name = entry.getKey();
            var executor = entry.getValue().getThreadPoolExecutor();

            int active = executor.getActiveCount();
            int poolSize = executor.getPoolSize();
            int queueSize = executor.getQueue().size();
            int queueCapacity = executor.getQueue().remainingCapacity() + queueSize;
            long completed = executor.getCompletedTaskCount();
            double queueUsage = queueCapacity > 0 ? (double) queueSize / queueCapacity : 0;

            builder.withDetail("pool." + name + ".active", active);
            builder.withDetail("pool." + name + ".poolSize", poolSize);
            builder.withDetail("pool." + name + ".queueSize", queueSize);
            builder.withDetail("pool." + name + ".queueCapacity", queueCapacity);
            builder.withDetail("pool." + name + ".completed", completed);

            if (queueUsage > 0.8) {
                degraded = true;
                log.warn("线程池 [{}] 队列使用率过高: {}%", name, String.format("%.1f", queueUsage * 100));
            }
        }

        return degraded ? builder.status("DEGRADED").build() : builder.build();
    }
}
