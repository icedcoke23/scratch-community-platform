package com.scratch.community.module.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.social.ai.*;
import com.scratch.community.module.social.entity.AiReview;
import com.scratch.community.module.social.mapper.AiReviewMapper;
import com.scratch.community.module.social.vo.AiReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * AI 点评服务
 *
 * 提供两种点评模式:
 * 1. RULE — 基于规则引擎的自动分析（MVP，默认）
 * 2. LLM  — 大语言模型点评（预留接口，后续接入）
 *
 * 评分维度:
 * - codeStructure (代码结构): 循环/条件/变量使用
 * - creativity (创意): 角色多样性/广播使用/自定义积木
 * - complexity (复杂度): 基于 sb3-parser 的复杂度评分
 * - readability (可读性): 积木数量/角色数量平衡
 * - bestPractice (最佳实践): 避免重复/合理使用事件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final AiReviewMapper aiReviewMapper;
    private final JdbcTemplate jdbcTemplate;
    private final CrossModuleQueryRepository crossModuleQuery;
    private final RuleBasedReviewEngine ruleEngine = new RuleBasedReviewEngine();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** LLM 提供商（可选，未配置时降级为规则引擎） */
    @Autowired(required = false)
    private LlmProvider llmProvider;

    /** 点评冷却时间（秒）- 同一项目 60 秒内不可重复点评 */
    private static final int REVIEW_COOLDOWN_SECONDS = 60;

    // ==================== 核心方法 ====================

    /**
     * 为项目生成 AI 点评
     *
     * 注意: @Transactional 加在此方法上（而非 doGenerateReview），
     * 因为 Spring AOP 基于代理，同类内调用不会触发事务。
     */
    @Transactional
    public AiReviewVO generateReview(Long userId, Long projectId) {
        // 1. 检查冷却时间
        AiReview latest = aiReviewMapper.selectLatestByProjectId(projectId);
        if (latest != null && latest.getCreatedAt() != null) {
            long secondsSinceLastReview = java.time.Duration.between(
                    latest.getCreatedAt(), java.time.LocalDateTime.now()).getSeconds();
            if (secondsSinceLastReview < REVIEW_COOLDOWN_SECONDS) {
                return toVO(latest);
            }
        }

        // 2. 生成点评（事务内）
        return doGenerateReview(userId, projectId);
    }

    /**
     * 从已有的 LLM 输出生成点评（SSE 流式完成后调用，不重复调用 LLM）
     *
     * @param userId    触发用户 ID
     * @param projectId 项目 ID
     * @param llmOutput LLM 已生成的完整文本
     */
    @Transactional
    public AiReviewVO generateReviewFromLlm(Long userId, Long projectId, String llmOutput) {
        java.util.Map<String, Object> project = getProjectData(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }

        int blockCount = project.get("block_count") != null ? ((Number) project.get("block_count")).intValue() : 0;
        double complexityScore = project.get("complexity_score") != null ? ((Number) project.get("complexity_score")).doubleValue() : 0.0;

        // 解析 LLM 输出
        ReviewResult result;
        boolean usedFallback = false;
        try {
            result = parseLlmResponse(llmOutput);
        } catch (Exception e) {
            log.warn("LLM 输出解析失败，降级到规则引擎: {}", e.getMessage());
            String parseResultJson = project.get("parse_result") != null ? project.get("parse_result").toString() : null;
            result = ruleEngine.generate(parseResultJson, blockCount, complexityScore);
            usedFallback = true;
        }

        // 保存
        AiReview review = new AiReview();
        review.setProjectId(projectId);
        review.setUserId(userId);
        review.setOverallScore(result.overallScore);
        try {
            review.setDimensionScores(objectMapper.writeValueAsString(result.dimensionScores));
            review.setStrengths(objectMapper.writeValueAsString(result.strengths));
            review.setSuggestions(objectMapper.writeValueAsString(result.suggestions));
        } catch (Exception e) {
            log.warn("序列化点评结果失败，使用默认值: {}", e.getMessage());
            review.setDimensionScores("{}");
            review.setStrengths("[]");
            review.setSuggestions("[]");
        }
        review.setSummary(result.summary);
        review.setDetail(result.detail);
        review.setBlockCount(blockCount);
        review.setSpriteCount(result.spriteCount);
        review.setComplexityScore(complexityScore);
        review.setProvider(usedFallback ? "LLM_FALLBACK" : "LLM");
        aiReviewMapper.insert(review);

        log.info("AI 点评保存: projectId={}, score={}, provider=LLM", projectId, result.overallScore);
        return toVO(review);
    }

    /**
     * 实际生成点评逻辑
     *
     * <p>策略: 优先使用 LLM（如果配置可用），失败时降级到规则引擎。
     * 注意: 不要加 @Transactional，由 generateReview() 提供事务上下文
     */
    protected AiReviewVO doGenerateReview(Long userId, Long projectId) {
        // 1. 检查项目是否存在且已发布
        java.util.Map<String, Object> project = getProjectData(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }

        // 2. 解析项目数据
        String parseResultJson = project.get("parse_result") != null ? project.get("parse_result").toString() : null;
        Integer blockCount = project.get("block_count") != null ? ((Number) project.get("block_count")).intValue() : 0;
        Double complexityScore = project.get("complexity_score") != null ? ((Number) project.get("complexity_score")).doubleValue() : 0.0;
        String projectName = project.get("title") != null ? project.get("title").toString() : "未命名项目";

        // 3. 尝试 LLM 点评，失败则降级到规则引擎
        ReviewResult result;
        String provider;

        if (llmProvider != null && llmProvider.isAvailable()) {
            try {
                result = generateLlmReview(projectName, blockCount, complexityScore, parseResultJson);
                provider = "LLM:" + llmProvider.getName();
                log.info("LLM 点评成功: projectId={}, provider={}", projectId, provider);
            } catch (Exception e) {
                log.warn("LLM 点评失败，降级到规则引擎: projectId={}, error={}", projectId, e.getMessage());
                result = ruleEngine.generate(parseResultJson, blockCount, complexityScore);
                provider = "RULE";
            }
        } else {
            result = ruleEngine.generate(parseResultJson, blockCount, complexityScore);
            provider = "RULE";
        }

        // 4. 保存点评
        AiReview review = new AiReview();
        review.setProjectId(projectId);
        review.setUserId(userId);
        review.setOverallScore(result.overallScore);
        try {
            review.setDimensionScores(objectMapper.writeValueAsString(result.dimensionScores));
            review.setStrengths(objectMapper.writeValueAsString(result.strengths));
            review.setSuggestions(objectMapper.writeValueAsString(result.suggestions));
        } catch (Exception e) {
            log.warn("序列化点评结果失败，使用默认值: {}", e.getMessage());
            review.setDimensionScores("{}");
            review.setStrengths("[]");
            review.setSuggestions("[]");
        }
        review.setSummary(result.summary);
        review.setDetail(result.detail);
        review.setBlockCount(blockCount);
        review.setSpriteCount(result.spriteCount);
        review.setComplexityScore(complexityScore);
        review.setProvider(provider);
        aiReviewMapper.insert(review);

        log.info("AI 点评生成: projectId={}, score={}, provider={}", projectId, result.overallScore, provider);
        return toVO(review);
    }

    /**
     * 使用 LLM 生成点评
     */
    private ReviewResult generateLlmReview(String projectName, int blockCount,
                                            double complexityScore, String parseResultJson) {
        String userMessage = AiReviewPrompt.buildUserMessage(
                projectName, blockCount, 0, complexityScore, parseResultJson);

        LlmResponse response = llmProvider.chat(AiReviewPrompt.SYSTEM_PROMPT, userMessage);

        if (!response.isSuccess()) {
            throw new LlmException(llmProvider.getName(), "LLM 返回空响应");
        }

        // 解析 LLM 返回的 JSON
        return parseLlmResponse(response.getContent());
    }

    /**
     * 解析 LLM 返回的 JSON 点评结果
     */
    private ReviewResult parseLlmResponse(String content) {
        try {
            // 清理可能的 markdown 代码块标记
            String json = content.trim();
            if (json.startsWith("```json")) json = json.substring(7);
            if (json.startsWith("```")) json = json.substring(3);
            if (json.endsWith("```")) json = json.substring(0, json.length() - 3);
            json = json.trim();

            JsonNode root = objectMapper.readTree(json);
            ReviewResult result = new ReviewResult();

            result.overallScore = root.has("overallScore") ? root.get("overallScore").asInt(3) : 3;
            result.overallScore = Math.max(1, Math.min(5, result.overallScore));

            // 解析维度评分
            Map<String, Integer> dims = new LinkedHashMap<>();
            JsonNode dimNode = root.get("dimensionScores");
            if (dimNode != null) {
                for (String key : List.of("codeStructure", "creativity", "complexity", "readability", "bestPractice")) {
                    if (dimNode.has(key)) {
                        dims.put(key, Math.max(1, Math.min(5, dimNode.get(key).asInt(3))));
                    }
                }
            }
            // 补全缺失的维度
            for (String key : List.of("codeStructure", "creativity", "complexity", "readability", "bestPractice")) {
                dims.putIfAbsent(key, 3);
            }
            result.dimensionScores = dims;

            result.summary = root.has("summary") ? root.get("summary").asText() : "AI 点评生成完成";
            result.detail = root.has("detail") ? root.get("detail").asText() : "";

            // 解析优点和建议
            result.strengths = parseStringList(root, "strengths");
            result.suggestions = parseStringList(root, "suggestions");

            if (result.strengths.isEmpty()) result.strengths.add("AI 分析完成");
            if (result.suggestions.isEmpty()) result.suggestions.add("继续加油！");

            return result;
        } catch (Exception e) {
            log.warn("解析 LLM 响应失败: {}", e.getMessage());
            throw new LlmException(llmProvider != null ? llmProvider.getName() : "unknown",
                    "解析 LLM 响应失败: " + e.getMessage(), e);
        }
    }

    private List<String> parseStringList(JsonNode root, String field) {
        List<String> list = new ArrayList<>();
        if (root.has(field) && root.get(field).isArray()) {
            for (JsonNode item : root.get(field)) {
                list.add(item.asText());
            }
        }
        return list;
    }

    /**
     * 获取项目最新的 AI 点评
     */
    @Transactional(readOnly = true)
    public AiReviewVO getLatestReview(Long projectId) {
        AiReview review = aiReviewMapper.selectLatestByProjectId(projectId);
        if (review == null) {
            return null;
        }
        return toVO(review);
    }

    /**
     * 获取项目的所有点评记录
     */
    @Transactional(readOnly = true)
    public List<AiReviewVO> getReviewHistory(Long projectId) {
        List<AiReview> reviews = aiReviewMapper.selectList(
                new LambdaQueryWrapper<AiReview>()
                        .eq(AiReview::getProjectId, projectId)
                        .orderByDesc(AiReview::getCreatedAt));
        return reviews.stream().map(this::toVO).toList();
    }

    // ==================== 工具方法 ====================

    private java.util.Map<String, Object> getProjectData(Long projectId) {
        try {
            return crossModuleQuery.getProjectInfo(projectId);
        } catch (Exception e) {
            return null;
        }
    }

    private AiReviewVO toVO(AiReview review) {
        AiReviewVO vo = new AiReviewVO();
        BeanUtils.copyProperties(review, vo);

        // 解析 JSON 字段
        try {
            if (review.getDimensionScores() != null) {
                vo.setDimensionScores(objectMapper.readValue(review.getDimensionScores(),
                        new TypeReference<Map<String, Integer>>() {}));
            }
        } catch (Exception e) { vo.setDimensionScores(new LinkedHashMap<>()); }

        try {
            if (review.getStrengths() != null) {
                vo.setStrengths(objectMapper.readValue(review.getStrengths(),
                        new TypeReference<List<String>>() {}));
            }
        } catch (Exception e) { vo.setStrengths(new ArrayList<>()); }

        try {
            if (review.getSuggestions() != null) {
                vo.setSuggestions(objectMapper.readValue(review.getSuggestions(),
                        new TypeReference<List<String>>() {}));
            }
        } catch (Exception e) { vo.setSuggestions(new ArrayList<>()); }

        return vo;
    }
}

