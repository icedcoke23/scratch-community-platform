package com.scratch.community.module.social.service;

import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.module.social.ai.LlmProvider;
import com.scratch.community.module.social.ai.LlmResponse;
import com.scratch.community.module.social.entity.AiReview;
import com.scratch.community.module.social.mapper.AiReviewMapper;
import com.scratch.community.module.social.vo.AiReviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiReviewService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AiReviewServiceTest {

    @InjectMocks
    private AiReviewService aiReviewService;

    @Mock
    private AiReviewMapper aiReviewMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CrossModuleQueryRepository crossModuleQuery;

    @Mock
    private LlmProvider llmProvider;

    @BeforeEach
    void setUp() {
        // 注入 LLM Provider（可选依赖）
        ReflectionTestUtils.setField(aiReviewService, "llmProvider", llmProvider);
    }

    private void mockProjectData() {
        when(crossModuleQuery.getProjectInfo(1L)).thenReturn(Map.of(
                "id", 1L,
                "user_id", 10L,
                "parse_result", "{\"spriteCount\":3,\"blockCount\":50,\"variableCount\":2}",
                "block_count", 50,
                "complexity_score", 25.0,
                "status", "published"
        ));
    }

    @Nested
    @DisplayName("规则引擎点评")
    class RuleBasedTests {

        @Test
        @DisplayName("正常生成规则点评")
        void generateReview_ruleBased() {
            mockProjectData();
            when(llmProvider.isAvailable()).thenReturn(false);
            when(aiReviewMapper.selectLatestByProjectId(1L)).thenReturn(null);
            when(aiReviewMapper.insert(any(AiReview.class))).thenReturn(1);

            AiReviewVO result = aiReviewService.generateReview(10L, 1L);

            assertNotNull(result);
            assertTrue(result.getOverallScore() >= 1 && result.getOverallScore() <= 5);
            assertNotNull(result.getSummary());
            assertNotNull(result.getStrengths());
            assertNotNull(result.getSuggestions());
            verify(aiReviewMapper).insert(argThat(review ->
                    "RULE".equals(review.getProvider())));
        }

        @Test
        @DisplayName("项目不存在抛异常")
        void generateReview_projectNotFound() {
            when(crossModuleQuery.getProjectInfo(999L)).thenReturn(null);
            when(aiReviewMapper.selectLatestByProjectId(999L)).thenReturn(null);

            assertThrows(BizException.class,
                    () -> aiReviewService.generateReview(10L, 999L));
        }
    }

    @Nested
    @DisplayName("LLM 点评")
    class LlmTests {

        @Test
        @DisplayName("LLM 可用时使用 LLM 点评")
        void generateReview_withLlm() {
            mockProjectData();
            when(llmProvider.isAvailable()).thenReturn(true);
            when(llmProvider.chat(anyString(), anyString())).thenReturn(
                    LlmResponse.builder()
                            .content("{\"overallScore\":4,\"dimensionScores\":{\"codeStructure\":4,\"creativity\":3,\"complexity\":4,\"readability\":3,\"bestPractice\":4},\"summary\":\"优秀作品\",\"detail\":\"详细点评\",\"strengths\":[\"代码结构好\"],\"suggestions\":[\"可以更复杂\"]}")
                            .totalTokens(100)
                            .build()
            );
            when(aiReviewMapper.selectLatestByProjectId(1L)).thenReturn(null);
            when(aiReviewMapper.insert(any(AiReview.class))).thenReturn(1);

            AiReviewVO result = aiReviewService.generateReview(10L, 1L);

            assertNotNull(result);
            assertEquals(4, result.getOverallScore());
            verify(aiReviewMapper).insert(argThat(review ->
                    review.getProvider().startsWith("LLM")));
        }

        @Test
        @DisplayName("LLM 失败降级到规则引擎")
        void generateReview_llmFallback() {
            mockProjectData();
            when(llmProvider.isAvailable()).thenReturn(true);
            when(llmProvider.chat(anyString(), anyString()))
                    .thenThrow(new RuntimeException("API 超时"));
            when(aiReviewMapper.selectLatestByProjectId(1L)).thenReturn(null);
            when(aiReviewMapper.insert(any(AiReview.class))).thenReturn(1);

            AiReviewVO result = aiReviewService.generateReview(10L, 1L);

            assertNotNull(result);
            // 降级后 provider 应为 RULE
            verify(aiReviewMapper).insert(argThat(review ->
                    "RULE".equals(review.getProvider())));
        }
    }

    @Nested
    @DisplayName("冷却时间")
    class CooldownTests {

        @Test
        @DisplayName("60 秒内重复请求返回已有结果")
        void generateReview_cooldown() {
            AiReview existing = new AiReview();
            existing.setId(1L);
            existing.setProjectId(1L);
            existing.setOverallScore(3);
            existing.setCreatedAt(LocalDateTime.now().minusSeconds(30));
            when(aiReviewMapper.selectLatestByProjectId(1L)).thenReturn(existing);

            AiReviewVO result = aiReviewService.generateReview(10L, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            // 不应调用 LLM 或插入新记录
            verify(llmProvider, never()).chat(anyString(), anyString());
            verify(aiReviewMapper, never()).insert(any());
        }

        @Test
        @DisplayName("超过 60 秒允许重新点评")
        void generateReview_afterCooldown() {
            AiReview existing = new AiReview();
            existing.setId(1L);
            existing.setProjectId(1L);
            existing.setOverallScore(3);
            existing.setCreatedAt(LocalDateTime.now().minusSeconds(61));
            when(aiReviewMapper.selectLatestByProjectId(1L)).thenReturn(existing);
            mockProjectData();
            when(llmProvider.isAvailable()).thenReturn(false);
            when(aiReviewMapper.insert(any(AiReview.class))).thenReturn(1);

            AiReviewVO result = aiReviewService.generateReview(10L, 1L);

            assertNotNull(result);
            verify(aiReviewMapper).insert(any());
        }
    }

    @Nested
    @DisplayName("generateReviewFromLlm()")
    class FromLlmTests {

        @Test
        @DisplayName("从 LLM 输出生成点评")
        void fromLlmOutput() {
            mockProjectData();
            when(aiReviewMapper.insert(any(AiReview.class))).thenReturn(1);

            String llmOutput = "{\"overallScore\":5,\"dimensionScores\":{\"codeStructure\":5,\"creativity\":4,\"complexity\":5,\"readability\":4,\"bestPractice\":5},\"summary\":\"出色作品\",\"detail\":\"非常好的项目\",\"strengths\":[\"结构清晰\",\"创意丰富\"],\"suggestions\":[\"可以添加更多角色\"]}";

            AiReviewVO result = aiReviewService.generateReviewFromLlm(10L, 1L, llmOutput);

            assertNotNull(result);
            assertEquals(5, result.getOverallScore());
            verify(aiReviewMapper).insert(argThat(review ->
                    "LLM".equals(review.getProvider())));
        }

        @Test
        @DisplayName("LLM 输出解析失败降级到规则引擎")
        void fromLlmOutput_parseFail() {
            mockProjectData();
            when(aiReviewMapper.insert(any(AiReview.class))).thenReturn(1);

            AiReviewVO result = aiReviewService.generateReviewFromLlm(10L, 1L, "invalid json");

            assertNotNull(result);
            verify(aiReviewMapper).insert(argThat(review ->
                    "LLM_FALLBACK".equals(review.getProvider())));
        }
    }
}

