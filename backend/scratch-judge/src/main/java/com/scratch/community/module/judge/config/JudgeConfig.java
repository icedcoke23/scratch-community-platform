package com.scratch.community.module.judge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 判题模块配置
 */
@Configuration
public class JudgeConfig {

    /**
     * RestTemplate Bean（用于调用 sandbox 服务）
     * 设置连接超时 5s，读取超时 35s（大于 sandbox 判题超时 30s）
     */
    @Bean
    public RestTemplate judgeRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(35000);
        return new RestTemplate(factory);
    }
}
