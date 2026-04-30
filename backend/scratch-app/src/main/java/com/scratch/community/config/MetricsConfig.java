package com.scratch.community.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义业务指标配置
 *
 * <p>提供以下自定义指标：
 * <ul>
 *   <li>scratch_user_register_total — 用户注册总数</li>
 *   <li>scratch_user_login_total — 用户登录总数</li>
 *   <li>scratch_project_create_total — 项目创建总数</li>
 *   <li>scratch_project_publish_total — 项目发布总数</li>
 *   <li>scratch_judge_submit_total — 判题提交总数</li>
 *   <li>scratch_judge_duration — 判题耗时分布</li>
 * </ul>
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter userRegisterCounter(MeterRegistry registry) {
        return Counter.builder("scratch_user_register_total")
                .description("用户注册总数")
                .register(registry);
    }

    @Bean
    public Counter userLoginCounter(MeterRegistry registry) {
        return Counter.builder("scratch_user_login_total")
                .description("用户登录总数")
                .register(registry);
    }

    @Bean
    public Counter projectCreateCounter(MeterRegistry registry) {
        return Counter.builder("scratch_project_create_total")
                .description("项目创建总数")
                .register(registry);
    }

    @Bean
    public Counter projectPublishCounter(MeterRegistry registry) {
        return Counter.builder("scratch_project_publish_total")
                .description("项目发布总数")
                .register(registry);
    }

    @Bean
    public Counter judgeSubmitCounter(MeterRegistry registry) {
        return Counter.builder("scratch_judge_submit_total")
                .description("判题提交总数")
                .register(registry);
    }

    @Bean
    public Timer judgeDurationTimer(MeterRegistry registry) {
        return Timer.builder("scratch_judge_duration")
                .description("判题耗时")
                .register(registry);
    }
}
