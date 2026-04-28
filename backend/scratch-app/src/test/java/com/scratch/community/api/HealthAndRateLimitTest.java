package com.scratch.community.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 健康检查 + 限流 API 集成测试
 */
@ApiIntegrationTest
@DisplayName("健康检查与限流集成测试")
class HealthAndRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("健康检查端点可访问")
    void healthCheck() throws Exception {
        // 测试环境 Redis/MySQL 为 mock，health 可能返回 503
        // 只验证端点可达（不返回 404/500）
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 200 || status == 503,
                            "Health endpoint should return 200 or 503, got: " + status);
                });
    }

    @Test
    @DisplayName("Swagger UI 可访问")
    void swaggerAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("不存在的 API → 404")
    void notFound() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("未认证访问受保护端点 → 401")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isUnauthorized());
    }
}
