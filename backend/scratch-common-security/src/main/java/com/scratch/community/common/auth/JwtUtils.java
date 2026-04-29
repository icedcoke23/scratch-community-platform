package com.scratch.community.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 *
 * <p>安全要求:
 * <ul>
 *   <li>密钥必须通过 {@code scratch.jwt.secret} 配置或 {@code JWT_SECRET} 环境变量设置</li>
 *   <li>密钥长度至少 32 字节 (256 位)</li>
 *   <li>生产环境建议使用随机生成的 Base64 编码密钥: {@code openssl rand -base64 32}</li>
 *   <li>生产环境硬性拒绝默认密钥，启动时直接报错</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * 开发环境默认密钥（生产环境禁止使用）
     * <p>注意: 此常量仅用于开发环境 fallback，生产环境必须通过环境变量配置
     */
    private static final String DEV_DEFAULT_SECRET = "scratch-community-secret-key-at-least-32bytes";

    /** 已知的默认密钥列表（用于安全校验） */
    private static final String[] KNOWN_DEFAULTS = {
        DEV_DEFAULT_SECRET,
        "scratch-community-secret-key-at-least-32bytes-long!!"
    };

    @Value("${scratch.jwt.secret:}")
    private String secret;

    @Value("${scratch.jwt.refresh-secret:}")
    private String refreshSecret;

    @Value("${scratch.jwt.expiration:86400000}")
    private long expiration; // 默认 24 小时

    @Value("${scratch.jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // 默认 7 天

    private boolean isDefaultSecret;

    @PostConstruct
    public void validateConfig() {
        // 检查是否使用了默认密钥
        boolean isBlank = secret == null || secret.isBlank();
        boolean isKnownDefault = false;
        for (String defaultKey : KNOWN_DEFAULTS) {
            if (defaultKey.equals(secret)) {
                isKnownDefault = true;
                break;
            }
        }

        if (isBlank || isKnownDefault) {
            String profile = System.getProperty("spring.profiles.active",
                    System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));
            if ("prod".equals(profile) || "production".equals(profile)) {
                throw new IllegalStateException(
                        "\n🚨🚨🚨 安全告警 🚨🚨🚨\n" +
                        "生产环境禁止使用默认 JWT 密钥！\n" +
                        "请设置环境变量: export JWT_SECRET=$(openssl rand -base64 32)\n" +
                        "或在 application-prod.yml 中配置 scratch.jwt.secret\n");
            }
            log.warn("⚠️ JWT 密钥使用了默认值或未配置！请设置 scratch.jwt.secret 为随机密钥。" +
                    "生成方法: openssl rand -base64 32");
            secret = DEV_DEFAULT_SECRET;
            isDefaultSecret = true;
        } else {
            isDefaultSecret = false;
        }

        // 密钥长度强制校验（所有环境）
        int keyLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (keyLength < 32) {
            throw new IllegalStateException(
                    "🚨 JWT 密钥长度不足 32 字节！当前长度: " + keyLength +
                    "。请设置 scratch.jwt.secret 为至少 32 字节的随机密钥。");
        }

        // Refresh Token 密钥校验
        if (refreshSecret == null || refreshSecret.isBlank()) {
            log.warn("⚠️ JWT Refresh Token 密钥未配置，将使用默认值。请设置 scratch.jwt.refresh-secret");
            refreshSecret = "scratch-community-refresh-secret-key-at-least-32b";
        }

        log.info("✅ JWT 配置校验通过 (密钥长度: {} 字节, 默认密钥: {}, Token 有效期: {}h, Refresh Token 有效期: {}h)",
                keyLength, isDefaultSecret, expiration / 3600000, refreshExpiration / 3600000);
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getKey())
                .compact();
    }

    /**
     * 生成 Refresh Token
     *
     * @param userId 用户 ID
     * @return Refresh Token
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getRefreshKey())
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 验证 Refresh Token
     *
     * @param token Refresh Token
     * @return 是否有效
     */
    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 解析 Refresh Token
     */
    private Claims parseRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(getRefreshKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Refresh Token 中获取用户 ID
     *
     * @param token Refresh Token
     * @return 用户 ID
     */
    public Long getUserIdFromRefreshToken(String token) {
        Claims claims = parseRefreshToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 获取 Refresh Token 的过期时间
     *
     * @param token Refresh Token
     * @return 过期时间（Date）
     */
    public Date getRefreshTokenExpiry(String token) {
        Claims claims = parseRefreshToken(token);
        return claims.getExpiration();
    }

    /**
     * 获取新 Refresh Token 的过期时间（基于当前时间 + 配置的过期时长）
     *
     * <p>用于注册/登录时直接获取过期时间，无需先生成 Token 再解析。
     *
     * @return 过期时间（Date）
     */
    public Date getRefreshTokenExpiryDate() {
        return new Date(System.currentTimeMillis() + refreshExpiration);
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从 Token 中获取角色
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 计算 Token 剩余有效期（毫秒）
     * <p>自动识别 Access Token 和 Refresh Token
     */
    public long getRemainingExpiry(String token) {
        try {
            Claims claims;
            // 尝试按 Refresh Token 解析（HS512）
            try {
                claims = parseRefreshToken(token);
            } catch (Exception e) {
                // 降级按 Access Token 解析（HS256）
                claims = parseToken(token);
            }
            Date exp = claims.getExpiration();
            long remaining = exp.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
