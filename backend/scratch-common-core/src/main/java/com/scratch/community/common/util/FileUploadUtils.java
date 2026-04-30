package com.scratch.community.common.util;

import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件上传工具类
 * 支持文件大小校验 + 类型白名单 + MinIO 上传
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadUtils {

    private final MinioClient minioClient;

    @Value("${scratch.minio.bucket-prefix:scratch-}")
    private String bucketPrefix;

    /** 默认最大文件大小：50MB（引用 FileConstants 统一常量） */
    private static final long DEFAULT_MAX_SIZE = FileConstants.DEFAULT_MAX_SIZE;

    /** sb3 文件最大大小：100MB（引用 FileConstants 统一常量） */
    private static final long SB3_MAX_SIZE = FileConstants.SB3_MAX_SIZE;

    /** 允许的图片类型 */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /** 允许的 sb3 类型（ZIP 格式） */
    private static final Set<String> ALLOWED_SB3_TYPES = Set.of(
            "application/zip", "application/x-zip-compressed", "application/octet-stream"
    );

    /**
     * 上传文件到 MinIO（默认 50MB 限制，不校验类型）
     */
    public String upload(MultipartFile file, String business) {
        return upload(file, business, DEFAULT_MAX_SIZE, null);
    }

    /**
     * 上传文件到 MinIO（带大小和类型校验）
     */
    public String upload(MultipartFile file, String business, long maxSize, Set<String> allowedTypes) {
        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR.getCode(), "文件不能为空");
        }

        // 2. 大小校验
        if (file.getSize() > maxSize) {
            long maxSizeMB = maxSize / (1024 * 1024);
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR.getCode(),
                    "文件大小超过限制，最大允许 " + maxSizeMB + "MB");
        }

        // 3. 类型校验
        if (allowedTypes != null && !allowedTypes.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || !allowedTypes.contains(contentType)) {
                throw new BizException(ErrorCode.FILE_UPLOAD_ERROR.getCode(),
                        "不支持的文件类型: " + contentType);
            }
        }

        // 4. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = business + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

        // 5. 上传到 MinIO
        String bucket = bucketPrefix + business;
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("文件上传成功: bucket={}, key={}, size={}", bucket, key, file.getSize());
            return key;
        } catch (Exception e) {
            log.error("文件上传失败: business={}, error={}", business, e.getMessage(), e);
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传字节数组到 MinIO（用于 base64 解码后的数据、程序生成的文件等）
     *
     * @param data     文件字节数组
     * @param business 业务类型（如 sb3、avatar）
     * @param filename 原始文件名（用于生成扩展名）
     * @return MinIO 对象 key
     */
    public String uploadBytes(byte[] data, String business, String filename) {
        if (data == null || data.length == 0) {
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR.getCode(), "数据不能为空");
        }

        // 生成唯一文件名
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."));
        }
        String key = business + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

        String bucket = bucketPrefix + business;
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType("application/octet-stream")
                            .build()
            );

            log.info("字节数据上传成功: bucket={}, key={}, size={}", bucket, key, data.length);
            return key;
        } catch (Exception e) {
            log.error("字节数据上传失败: business={}, error={}", business, e.getMessage(), e);
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR.getCode(), "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传头像（校验图片类型 + 5MB 限制）
     */
    public String uploadAvatar(MultipartFile file) {
        return upload(file, "avatar", FileConstants.AVATAR_MAX_SIZE, ALLOWED_IMAGE_TYPES);
    }

    /**
     * 上传 sb3 文件（校验大小 100MB）
     */
    public String uploadSb3(MultipartFile file) {
        return upload(file, "sb3", SB3_MAX_SIZE, ALLOWED_SB3_TYPES);
    }

    /**
     * 获取文件的预签名下载 URL（有效期 7 天）
     */
    public String getUrl(String business, String key) {
        String bucket = bucketPrefix + business;
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取文件 URL 失败: bucket={}, key={}, error={}", bucket, key, e.getMessage(), e);
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR.getCode(), "获取文件 URL 失败");
        }
    }

    /**
     * 根据已存储的 presigned URL 生成新的 presigned URL（指定有效期）
     *
     * <p>用于 TurboWarp project_url 等场景，需要短期有效的可公开访问链接。
     * 从已存储的 presigned URL 中解析 bucket 和 key，生成新的短期 URL。
     *
     * @param storedUrl 已存储的 presigned URL
     * @param expiry    有效期
     * @param unit      时间单位
     * @return 新的 presigned URL
     */
    public String refreshPresignedUrl(String storedUrl, int expiry, TimeUnit unit) {
        try {
            java.net.URI uri = new java.net.URI(storedUrl);
            String path = uri.getPath(); // /bucket/key
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            int slashIdx = path.indexOf('/');
            if (slashIdx <= 0 || slashIdx >= path.length() - 1) {
                log.warn("无法从 URL 解析 bucket/key: {}", storedUrl);
                return storedUrl;
            }
            String bucket = path.substring(0, slashIdx);
            String key = path.substring(slashIdx + 1);

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(expiry, unit)
                            .build()
            );
        } catch (Exception e) {
            log.warn("刷新 presigned URL 失败，返回原 URL: {}", e.getMessage());
            return storedUrl;
        }
    }

    /**
     * 获取文件的预签名下载 URL（自定义有效期）
     *
     * @param business    业务类型（如 sb3、avatar）
     * @param key         对象 key
     * @param expiryHours 有效期（小时）
     * @return 预签名 URL
     */
    public String getPresignedUrl(String business, String key, int expiryHours) {
        String bucket = bucketPrefix + business;
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(expiryHours, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取预签名 URL 失败: bucket={}, key={}, error={}", bucket, key, e.getMessage(), e);
            throw new BizException(ErrorCode.FILE_UPLOAD_ERROR.getCode(), "获取文件 URL 失败");
        }
    }

    /**
     * 删除文件
     */
    public void delete(String business, String key) {
        String bucket = bucketPrefix + business;
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            log.info("文件删除成功: bucket={}, key={}", bucket, key);
        } catch (Exception e) {
            log.error("文件删除失败: bucket={}, key={}, error={}", bucket, key, e.getMessage(), e);
        }
    }
}
