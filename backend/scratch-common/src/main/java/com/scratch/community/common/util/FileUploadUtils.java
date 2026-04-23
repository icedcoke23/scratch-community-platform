package com.scratch.community.common.util;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 文件上传工具 (MinIO)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadUtils {

    private final MinioClient minioClient;
    private final com.scratch.community.common.config.MinioConfig minioConfig;

    /**
     * 上传文件
     * @param file   文件
     * @param bucket bucket 名称 (不含前缀)
     * @return 文件访问 key
     */
    public String upload(MultipartFile file, String bucket) {
        String fullBucket = minioConfig.getBucketPrefix() + bucket;
        String key = generateKey(file.getOriginalFilename());

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(fullBucket)
                            .object(key)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return key;
        } catch (Exception e) {
            log.error("文件上传失败: bucket={}, key={}", fullBucket, key, e);
            throw new com.scratch.community.common.exception.BizException(20004, "文件上传失败");
        }
    }

    /**
     * 获取文件访问 URL
     */
    public String getUrl(String bucket, String key) {
        String fullBucket = minioConfig.getBucketPrefix() + bucket;
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(fullBucket)
                            .object(key)
                            .method(io.minio.http.Method.GET)
                            .expiry(7 * 24 * 3600) // 7 天有效
                            .build()
            );
        } catch (Exception e) {
            log.error("获取文件 URL 失败: bucket={}, key={}", fullBucket, key, e);
            return null;
        }
    }

    /**
     * 删除文件
     */
    public void delete(String bucket, String key) {
        String fullBucket = minioConfig.getBucketPrefix() + bucket;
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(fullBucket)
                            .object(key)
                            .build()
            );
        } catch (Exception e) {
            log.error("文件删除失败: bucket={}, key={}", fullBucket, key, e);
        }
    }

    /**
     * 生成唯一文件 key
     */
    private String generateKey(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }
}
