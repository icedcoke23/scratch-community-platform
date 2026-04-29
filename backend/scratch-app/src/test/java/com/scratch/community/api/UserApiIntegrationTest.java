package com.scratch.community.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * 用户 API 集成测试
 *
 * 测试链路: 注册 → 登录 → 获取信息 → 更新 → 修改密码
 */
@ApiIntegrationTest
@DisplayName("用户 API 集成测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // 注册测试用户
        String registerJson = """
                {
                    "username": "testuser_%d",
                    "password": "Test@1234",
                    "nickname": "测试用户",
                    "role": "STUDENT"
                }
                """.formatted(System.nanoTime());

        MvcResult registerResult = mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        // 登录获取 Token
        String username = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("data").get("userInfo").get("username").asText();

        String loginJson = """
                {
                    "username": "%s",
                    "password": "Test@1234"
                }
                """.formatted(username);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        authToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("token").asText();
    }

    @Nested
    @DisplayName("注册")
    class RegisterTests {

        @Test
        @DisplayName("正常注册 → 返回用户信息")
        void register_success() throws Exception {
            String json = """
                    {
                        "username": "newuser_%d",
                        "password": "Pass@1234",
                        "nickname": "新用户",
                        "role": "STUDENT"
                    }
                    """.formatted(System.nanoTime());

            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.userInfo.username").isNotEmpty())
                    .andExpect(jsonPath("$.data.userInfo.nickname").value("新用户"));
        }

        @Test
        @DisplayName("用户名重复 → 返回错误")
        void register_duplicate() throws Exception {
            String json = """
                    {
                        "username": "duplicate_user",
                        "password": "Pass@1234",
                        "nickname": "用户1"
                    }
                    """;

            // 第一次注册
            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());

            // 第二次注册同名用户
            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }

        @Test
        @DisplayName("密码太弱 → 返回校验错误")
        void register_weakPassword() throws Exception {
            String json = """
                    {
                        "username": "weakpwd_user",
                        "password": "123",
                        "nickname": "弱密码"
                    }
                    """;

            mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("登录")
    class LoginTests {

        @Test
        @DisplayName("正确凭据 → 返回 Token")
        void login_success() throws Exception {
            // 先注册
            String regJson = """
                    {
                        "username": "login_test_%d",
                        "password": "Test@1234",
                        "nickname": "登录测试"
                    }
                    """.formatted(System.nanoTime());

            MvcResult regResult = mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(regJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String username = objectMapper.readTree(regResult.getResponse().getContentAsString())
                    .get("data").get("userInfo").get("username").asText();

            String loginJson = """
                    {
                        "username": "%s",
                        "password": "Test@1234"
                    }
                    """.formatted(username);

            mockMvc.perform(post("/api/v1/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.userInfo").isNotEmpty());
        }

        @Test
        @DisplayName("错误密码 → 返回错误")
        void login_wrongPassword() throws Exception {
            String loginJson = """
                    {
                        "username": "nonexistent_%d",
                        "password": "WrongPass"
                    }
                    """.formatted(System.nanoTime());

            mockMvc.perform(post("/api/v1/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(0)));
        }
    }

    @Nested
    @DisplayName("个人信息")
    class ProfileTests {

        @Test
        @DisplayName("获取当前用户信息 → 需要 Token")
        void me_withToken() throws Exception {
            try {
                mockMvc.perform(get("/api/v1/user/me")
                                .header("Authorization", "Bearer " + authToken))
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isOk());
            } catch (Exception e) {
                MvcResult result = e.getMessage() != null ? null : null;
                System.err.println("=== me_withToken FAILED ===");
                e.printStackTrace();
                throw e;
            }
            mockMvc.perform(get("/api/v1/user/me")
                            .header("Authorization", "Bearer " + authToken))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.username").isNotEmpty())
                    .andExpect(jsonPath("$.data.nickname").isNotEmpty());
        }

        @Test
        @DisplayName("无 Token → 返回 401")
        void me_withoutToken() throws Exception {
            mockMvc.perform(get("/api/v1/user/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("无效 Token → 返回 401")
        void me_invalidToken() throws Exception {
            mockMvc.perform(get("/api/v1/user/me")
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("登出与刷新")
    class LogoutRefreshTests {

        private String authToken;
        private String refreshToken;

        @BeforeEach
        void setUp() throws Exception {
            // 独立注册并登录，获取 tokens
            String username = "logout_refresh_%d".formatted(System.nanoTime());
            String regJson = """
                    {
                        "username": "%s",
                        "password": "Test@1234",
                        "nickname": "登出刷新测试",
                        "role": "STUDENT"
                    }
                    """.formatted(username);

            MvcResult regResult = mockMvc.perform(post("/api/v1/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(regJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginUsername = objectMapper.readTree(regResult.getResponse().getContentAsString())
                    .get("data").get("userInfo").get("username").asText();

            String loginJson = """
                    {
                        "username": "%s",
                        "password": "Test@1234"
                    }
                    """.formatted(loginUsername);

            MvcResult loginResult = mockMvc.perform(post("/api/v1/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();

            // 提取 token 和 refreshToken
            var jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
            authToken = jsonNode.get("data").get("token").asText();
            refreshToken = jsonNode.get("data").get("refreshToken").asText();
        }

        @Test
        @DisplayName("登出 → Token 失效")
        void logout_invalidatesToken() throws Exception {
            // 登出
            mockMvc.perform(post("/api/v1/user/logout")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk());

            // 使用已登出的 Token 访问 → 应失败
            mockMvc.perform(get("/api/v1/user/me")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("刷新 Token → 返回新 Token")
        void refresh_returnsNewToken() throws Exception {
            // 使用 refreshToken 在请求体中（不是 Authorization header）
            String refreshBody = "{\"refreshToken\":\"%s\"}".formatted(refreshToken);

            MvcResult result = mockMvc.perform(post("/api/v1/user/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andReturn();

            String newToken = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("data").get("token").asText();

            // 新 Token 可用
            mockMvc.perform(get("/api/v1/user/me")
                            .header("Authorization", "Bearer " + newToken))
                    .andExpect(status().isOk());

            // 旧 Refresh Token 失效（不能再次刷新）
            mockMvc.perform(post("/api/v1/user/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(refreshBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(com.scratch.community.common.result.ErrorCode.REFRESH_TOKEN_INVALID.getCode()));
        }
    }
}
