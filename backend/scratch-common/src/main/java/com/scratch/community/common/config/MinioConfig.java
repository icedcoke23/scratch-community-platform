package com.scratch.community.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置
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
