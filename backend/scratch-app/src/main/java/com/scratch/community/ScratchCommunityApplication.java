package com.scratch.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scratch Community Platform 启动类
 */
@SpringBootApplication(scanBasePackages = "com.scratch.community")
@MapperScan("com.scratch.community.module.*.mapper")
@EnableAsync
@EnableScheduling
public class ScratchCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScratchCommunityApplication.class, args);
    }
}
