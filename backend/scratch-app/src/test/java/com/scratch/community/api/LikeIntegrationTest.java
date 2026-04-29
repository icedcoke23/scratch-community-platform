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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.hamcrest.Matchers.*;

/**
 * 点赞集成测试（独立类）
 *
 * 测试链路: 创建用户 → 创建项目 → 点赞 → 取消 → 重新点赞
 */
@ApiIntegrationTest
@DisplayName("点赞集成测试")
class LikeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long projectId;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // 创建用户并登录
        String username = "like_integration_" + System.nanoTime();
        String password = "Test@1234";

        // 注册
        MvcResult regResult = mockMvc.perform(post("/api/v1/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"nickname\":\"点赞集成用户\",\"role\":\"STUDENT\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String loginUsername = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("data").get("userInfo").get("username").asText();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + loginUsername + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        var loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        userToken = loginNode.get("data").get("token").asText();

        // 创建项目
        String projectJson = """
            {
              "title": "点赞集成测试项目",
              "description": "用于点赞集成测试",
              "thumbnail": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
              "code": "like_test",
              "isPublic": true,
              "tags": ["测试"]
            }
            """;
        MvcResult projectResult = mockMvc.perform(post("/api/v1/project")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        projectId = objectMapper.readTree(projectResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }

    @Test
    @DisplayName("点赞 → 取消点赞 → 重新点赞")
    void likeUnlikeRelike() throws Exception {
        // 1. 点赞（首次，新增）
        mockMvc.perform(post("/api/v1/social/project/" + projectId + "/like")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        // 2. 取消点赞（有记录可删，真正 execute）
        mockMvc.perform(delete("/api/v1/social/project/" + projectId + "/like")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));

        // 3. 重新点赞（已取消，可再次新增）
        mockMvc.perform(post("/api/v1/social/project/" + projectId + "/like")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }
}
