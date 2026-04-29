package com.scratch.community.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * MinIO 配置
 *
 * <p>安全要求:
 * <ul>
 *   <li>生产环境必须通过环境变量修改默认凭证 (minioadmin/minioadmin)</li>
 *   <li>开发环境允许默认值，但会记录警告日志</li>
 * </ul>
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "scratch.minio")
public class MinioConfig {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucketPrefix = "scratch-";

    @PostConstruct
    public void validateCredentials() {
        // 检查空值
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalStateException("MinIO accessKey 未配置，请设置 scratch.minio.access-key 或 MINIO_ROOT_USER 环境变量");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("MinIO secretKey 未配置，请设置 scratch.minio.secret-key 或 MINIO_ROOT_PASSWORD 环境变量");
        }

        // 检查默认凭证
        boolean isDefault = "minioadmin".equals(accessKey) && "minioadmin".equals(secretKey);
        if (isDefault) {
            String profile = System.getProperty("spring.profiles.active",
                    System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));
            if ("prod".equals(profile) || "production".equals(profile)) {
                throw new IllegalStateException(
                        "\n🚨🚨🚨 安全告警 🚨🚨🚨\n" +
                        "生产环境禁止使用 MinIO 默认凭证 (minioadmin/minioadmin)！\n" +
                        "请设置环境变量: export MINIO_ROOT_USER=<新用户名>\n" +
                        "           export MINIO_ROOT_PASSWORD=<新密码>\n" +
                        "或在 application-prod.yml 中配置 scratch.minio.access-key/secret-key\n");
            }
            log.warn("⚠️ MinIO 使用了默认凭证 (minioadmin/minioadmin)，生产环境必须修改！");
        } else {
            log.info("✅ MinIO 凭证校验通过 (accessKey: {}***)", mask(accessKey));
        }
    }

    private String mask(String str) {
        if (str == null || str.length() < 3) return "***";
        return str.substring(0, 2) + "***";
    }

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // 确保 bucket 存在
        ensureBucket(client, bucketPrefix + "sb3");
        ensureBucket(client, bucketPrefix + "avatar");
        ensureBucket(client, bucketPrefix + "cover");

        return client;
    }

    private void ensureBucket(MinioClient client, String bucket) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("创建 MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("检查/创建 bucket 失败: {} - {}", bucket, e.getMessage());
        }
    }
}
