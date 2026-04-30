package com.scratch.community.module.editor.config;

import com.scratch.community.sb3.parser.SB3Parser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * editor 模块配置
 * 注册 scratch-sb3 的解析器为 Spring Bean
 */
@Configuration
public class EditorConfig {

    @Bean
    public SB3Parser sb3Parser() {
        return new SB3Parser();
    }
}
