package com.scratch.community.module.social.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 社区模块配置
 * 启用定时任务（排行榜刷新）
 */
@Configuration
@EnableScheduling
public class SocialConfig {
}
