package com.scratch.community.module.social.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.auth.SseTokenService;
import com.scratch.community.common.result.R;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.module.social.ai.LlmProvider;
import com.scratch.community.module.social.ai.AiReviewPrompt;
import com.scratch.community.module.social.service.AiReviewService;
import com.scratch.community.module.social.vo.AiReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * AI 点评 API
 */
@Slf4j
@Tag(name = "AI 点评", description = "项目 AI 智能点评")
@RestController
@RequestMapping("/api/v1/ai-review")
public class AiReviewController {

    private final AiReviewService aiReviewService;
    private final CrossModuleQueryRepository crossModuleQuery;
    private final ObjectMapper objectMapper;
    private final SseTokenService sseTokenService;

    @Autowired(required = false)
    private LlmProvider llmProvider;

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("taskExecutor")
    private Executor sseExecutor;

    public AiReviewController(
            AiReviewService aiReviewService,
            CrossModuleQueryRepository crossModuleQuery,
            ObjectMapper objectMapper,
            SseTokenService sseTokenService) {
        this.aiReviewService = aiReviewService;
        this.crossModuleQuery = crossModuleQuery;
        this.objectMapper = objectMapper;
        this.sseTokenService = sseTokenService;
    }

    @Operation(summary = "生成 AI 点评")
    @PostMapping("/project/{projectId}")
    public R<AiReviewVO> generateReview(@PathVariable Long projectId) {
        return R.ok(aiReviewService.generateReview(LoginUser.getUserId(), projectId));
    }

    /**
     * 获取 SSE 一次性 Token
     *
     * <p>客户端在建立 SSE 连接前，先调用此接口获取一次性 Token，
     * 然后用 {@code ?sse_token=xxx} 建立 SSE 连接，避免 JWT Token 出现在 URL 中。
     *
     * @return 一次性 Token（有效期 5 分钟）
     */
    @Operation(summary = "获取 SSE 一次性 Token")
    @GetMapping("/sse-token")
    public R<String> getSseToken() {
        Long userId = LoginUser.getUserId();
        String token = sseTokenService.generateToken(userId);
        return R.ok(token);
    }

    /**
     * SSE 流式 AI 点评
     *
     * <p>逐步返回 LLM 生成的点评内容，前端可通过 EventSource 实时展示。
     * 如果 LLM 不可用，自动降级为非流式规则引擎点评。
     *
     * <p>修复记录:
     * - 项目数据现在从 CrossModuleQueryRepository 获取（而非硬编码）
     * - onComplete 不再重复调用 generateReview，直接返回已有结果
     * - 线程池使用共享 taskExecutor（有上限）
     */
    @Operation(summary = "流式 AI 点评 (SSE)")
    @GetMapping(value = "/project/{projectId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReview(@PathVariable Long projectId) {
        SseEmitter emitter = new SseEmitter(60000L);

        sseExecutor.execute(() -> {
            try {
                // 获取真实项目数据（而非硬编码）
                Map<String, Object> projectInfo = crossModuleQuery.getProjectInfo(projectId);
                if (projectInfo == null) {
                    emitter.send(SseEmitter.event().name("error").data("项目不存在"));
                    emitter.complete();
                    return;
                }

                String projectName = "项目 #" + projectId;
                int blockCount = projectInfo.get("block_count") != null
                        ? ((Number) projectInfo.get("block_count")).intValue() : 0;
                double complexityScore = projectInfo.get("complexity_score") != null
                        ? ((Number) projectInfo.get("complexity_score")).doubleValue() : 0;
                String parseResult = (String) projectInfo.get("parse_result");

                if (llmProvider == null || !llmProvider.isAvailable()) {
                    // 降级：规则引擎一次性返回
                    AiReviewVO review = aiReviewService.generateReview(LoginUser.getUserId(), projectId);
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data(objectMapper.writeValueAsString(review)));
                    emitter.complete();
                    return;
                }

                String userMessage = AiReviewPrompt.buildUserMessage(
                        projectName, blockCount, 0, complexityScore, parseResult);

                llmProvider.chatStream(AiReviewPrompt.SYSTEM_PROMPT, userMessage, new LlmProvider.StreamCallback() {
                    @Override
                    public void onToken(String token) {
                        try {
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (Exception e) {
                            log.warn("SSE 发送 token 失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete(com.scratch.community.module.social.ai.LlmResponse response) {
                        try {
                            // 流式完成后，用 LLM 的内容生成点评（不重复调用 LLM）
                            AiReviewVO review = aiReviewService.generateReviewFromLlm(
                                    LoginUser.getUserId(), projectId, response.getContent());
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data(objectMapper.writeValueAsString(review)));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("SSE 完成处理失败: {}", e.getMessage());
                            // 降级到规则引擎
                            try {
                                AiReviewVO review = aiReviewService.generateReview(
                                        LoginUser.getUserId(), projectId);
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data(objectMapper.writeValueAsString(review)));
                                emitter.complete();
                            } catch (Exception ex) {
                                emitter.completeWithError(ex);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("SSE 流式点评失败: {}", error.getMessage());
                        try {
                            AiReviewVO review = aiReviewService.generateReview(
                                    LoginUser.getUserId(), projectId);
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data(objectMapper.writeValueAsString(review)));
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    }
                });

            } catch (Exception e) {
                log.error("SSE 点评异常: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> log.debug("SSE 点评连接关闭: projectId={}", projectId));
        emitter.onTimeout(() -> { log.warn("SSE 点评超时: projectId={}", projectId); emitter.complete(); });

        return emitter;
    }

    @Operation(summary = "获取项目最新点评")
    @GetMapping("/project/{projectId}")
    public R<AiReviewVO> getLatestReview(@PathVariable Long projectId) {
        AiReviewVO review = aiReviewService.getLatestReview(projectId);
        return R.ok(review);
    }

    @Operation(summary = "获取项目点评历史")
    @GetMapping("/project/{projectId}/history")
    public R<List<AiReviewVO>> getReviewHistory(@PathVariable Long projectId) {
        return R.ok(aiReviewService.getReviewHistory(projectId));
    }
}
