package com.scratch.community.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * 社区 API 集成测试
 *
 * 测试链路: 注册用户 → 创建项目 → 点赞 → 评论 → 排行榜
 */
@ApiIntegrationTest
@DisplayName("社区 API 集成测试")
class SocialApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private Long projectId;

    @BeforeEach
    void setUp() throws Exception {
        // 注册并登录用户
        String regJson = """
                {
                    "username": "social_user_%d",
                    "password": "Test@1234",
                    "nickname": "社区用户",
                    "role": "STUDENT"
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

        MvcResult loginResult = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        userToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("data").get("token").asText();
    }

    @Nested
    @DisplayName("项目")
    class ProjectTests {

        @Test
        @DisplayName("创建项目 → 返回项目信息")
        void createProject() throws Exception {
            String json = """
                    {
                        "title": "我的第一个 Scratch 项目",
                        "description": "一个简单的动画",
                        "tags": "动画,入门"
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/api/v1/project")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.title").value("我的第一个 Scratch 项目"))
                    .andReturn();

            projectId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("data").get("id").asLong();
        }

        @Test
        @DisplayName("获取项目详情 → 返回完整信息")
        void getProjectDetail() throws Exception {
            // 先创建项目
            createProject();

            mockMvc.perform(get("/api/v1/project/" + projectId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.title").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("点赞")
    class LikeTests {

        @Test
        @DisplayName("点赞 → 取消点赞 → 重新点赞")
        void likeUnlikeRelike() throws Exception {
            // 先创建项目
            createProject();

            // 点赞
            mockMvc.perform(post("/api/v1/social/project/" + projectId + "/like")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // 取消点赞
            mockMvc.perform(delete("/api/v1/social/project/" + projectId + "/like")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // 重新点赞
            mockMvc.perform(post("/api/v1/social/project/" + projectId + "/like")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Nested
    @DisplayName("评论")
    class CommentTests {

        @Test
        @DisplayName("添加评论 → 查询评论列表")
        void addCommentAndGetList() throws Exception {
            // 先创建项目
            createProject();

            // 添加评论
            String commentJson = """
                    {
                        "projectId": %d,
                        "content": "这个项目做得真棒！"
                    }
                    """.formatted(projectId);

            mockMvc.perform(post("/api/v1/social/comment")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(commentJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // 查询评论列表
            mockMvc.perform(get("/api/v1/social/project/" + projectId + "/comments")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("信息流")
    class FeedTests {

        @Test
        @DisplayName("获取最新项目列表")
        void getLatestFeed() throws Exception {
            mockMvc.perform(get("/api/v1/social/feed")
                            .param("sort", "latest")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("获取最热项目列表")
        void getHotFeed() throws Exception {
            mockMvc.perform(get("/api/v1/social/feed")
                            .param("sort", "hot")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records").isArray());
        }
    }

    private void createProject() throws Exception {
        String json = """
                {
                    "title": "测试项目_%d",
                    "description": "用于测试"
                }
                """.formatted(System.nanoTime());

        MvcResult result = mockMvc.perform(post("/api/v1/project")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        projectId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }
}
