package com.scratch.community.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 自定义健康检查配置
 *
 * <p>提供以下健康检查：
 * <ul>
 *   <li>database — 数据库连接检查</li>
 *   <li>disk — 磁盘空间检查</li>
 * </ul>
 */
@Configuration
public class HealthConfig {

    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return () -> {
            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    return Health.up()
                            .withDetail("database", "MySQL")
                            .withDetail("status", "connected")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("database", "MySQL")
                        .withDetail("error", e.getMessage())
                        .build();
            }
            return Health.down()
                    .withDetail("database", "MySQL")
                    .withDetail("error", "Unknown error")
                    .build();
        };
    }

    @Bean
    public HealthIndicator diskSpaceHealthIndicator() {
        return () -> {
            java.io.File root = new java.io.File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usableSpace = root.getUsableSpace();
            double usagePercent = 100.0 * (totalSpace - freeSpace) / totalSpace;

            Health.Builder builder = usagePercent < 90 ? Health.up() : Health.down();
            return builder
                    .withDetail("total", formatSize(totalSpace))
                    .withDetail("free", formatSize(freeSpace))
                    .withDetail("usable", formatSize(usableSpace))
                    .withDetail("usage", String.format("%.1f%%", usagePercent))
                    .build();
        };
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
