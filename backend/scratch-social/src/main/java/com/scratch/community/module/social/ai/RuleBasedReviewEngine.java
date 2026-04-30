package com.scratch.community.module.social.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 基于规则的 AI 点评引擎
 *
 * <p>从 AiReviewService 中抽取的纯计算逻辑，不依赖任何 Spring Bean。
 * 负责解析 SB3 项目数据并生成多维度评分和评语。
 *
 * <p>评分维度:
 * - codeStructure (代码结构): 循环/条件/变量使用
 * - creativity (创意): 角色多样性/广播使用/自定义积木
 * - complexity (复杂度): 基于 sb3-parser 的复杂度评分
 * - readability (可读性): 积木数量/角色数量平衡
 * - bestPractice (最佳实践): 避免重复/合理使用事件
 */
@Slf4j
public class RuleBasedReviewEngine {

    /**
     * 基于规则的点评生成器
     *
     * @param parseResultJson SB3 解析结果 JSON
     * @param blockCount      积木总数
     * @param complexityScore 复杂度评分
     * @return 点评结果
     */
    public ReviewResult generate(String parseResultJson, Integer blockCount, Double complexityScore) {
        ReviewResult result = new ReviewResult();

        // 解析 SB3 数据
        int spriteCount = 0;
        int variableCount = 0;
        int listCount = 0;
        int broadcastCount = 0;
        Map<String, Integer> blockTypeCounts = new HashMap<>();

        if (parseResultJson != null && !parseResultJson.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode parseData = mapper.readTree(parseResultJson);
                spriteCount = parseData.has("spriteCount") ? parseData.get("spriteCount").asInt(0) : 0;
                variableCount = parseData.has("variableCount") ? parseData.get("variableCount").asInt(0) : 0;
                listCount = parseData.has("listCount") ? parseData.get("listCount").asInt(0) : 0;
                broadcastCount = parseData.has("broadcastCount") ? parseData.get("broadcastCount").asInt(0) : 0;

                JsonNode typeCounts = parseData.get("blockTypeCounts");
                if (typeCounts != null && typeCounts.isObject()) {
                    var iter = typeCounts.fields();
                    while (iter.hasNext()) {
                        var entry = iter.next();
                        blockTypeCounts.put(entry.getKey(), entry.getValue().asInt(0));
                    }
                }
            } catch (Exception e) {
                log.warn("解析项目数据失败: {}", e.getMessage());
            }
        }

        result.spriteCount = spriteCount;
        blockCount = blockCount != null ? blockCount : 0;
        complexityScore = complexityScore != null ? complexityScore : 0.0;

        // ===== 维度评分 =====
        Map<String, Integer> dims = new LinkedHashMap<>();

        // 1. 代码结构 (1-5): 基于控制流和变量使用
        int controlFlow = countOpcodes(blockTypeCounts, "control_");
        int dataBlocks = countOpcodes(blockTypeCounts, "data_") + variableCount + listCount;
        int structureScore = calculateStructureScore(blockCount, controlFlow, dataBlocks);
        dims.put("codeStructure", structureScore);

        // 2. 创意 (1-5): 角色多样性 + 广播 + 自定义积木
        int customBlocks = countOpcodes(blockTypeCounts, "procedures_");
        int penBlocks = countOpcodes(blockTypeCounts, "pen_");
        int musicBlocks = countOpcodes(blockTypeCounts, "music_");
        int creativityScore = calculateCreativityScore(spriteCount, broadcastCount, customBlocks, penBlocks, musicBlocks);
        dims.put("creativity", creativityScore);

        // 3. 复杂度 (1-5): 基于 sb3-parser 复杂度评分
        int complexityScoreInt = calculateComplexityDimension(complexityScore);
        dims.put("complexity", complexityScoreInt);

        // 4. 可读性 (1-5): 基于代码规模和角色平衡
        int readabilityScore = calculateReadabilityScore(blockCount, spriteCount, variableCount);
        dims.put("readability", readabilityScore);

        // 5. 最佳实践 (1-5): 事件驱动 + 避免重复
        int eventBlocks = countOpcodes(blockTypeCounts, "event_");
        int bestPracticeScore = calculateBestPracticeScore(blockCount, eventBlocks, controlFlow, spriteCount);
        dims.put("bestPractice", bestPracticeScore);

        result.dimensionScores = dims;

        // ===== 总体评分 =====
        double avg = dims.values().stream().mapToInt(Integer::intValue).average().orElse(3.0);
        result.overallScore = (int) Math.round(avg);

        // ===== 生成评语 =====
        result.summary = generateSummary(result.overallScore, dims);
        result.detail = generateDetail(result.overallScore, dims, blockCount, spriteCount, complexityScore);
        result.strengths = generateStrengths(dims, blockCount, spriteCount, variableCount, broadcastCount);
        result.suggestions = generateSuggestions(dims, blockCount, spriteCount, controlFlow, customBlocks);

        return result;
    }

    // ===== 评分计算 =====

    public int calculateStructureScore(int blockCount, int controlFlow, int dataBlocks) {
        if (blockCount == 0) return 1;
        double ratio = (double) (controlFlow + dataBlocks) / blockCount;
        if (ratio > 0.4) return 5;
        if (ratio > 0.3) return 4;
        if (ratio > 0.2) return 3;
        if (ratio > 0.1) return 2;
        return 1;
    }

    public int calculateCreativityScore(int sprites, int broadcasts, int custom, int pen, int music) {
        int score = 1;
        if (sprites >= 3) score++;
        if (broadcasts > 0) score++;
        if (custom > 0 || pen > 0) score++;
        if (music > 0 || sprites >= 5) score++;
        return Math.min(score, 5);
    }

    public int calculateComplexityDimension(double complexityScore) {
        if (complexityScore >= 60) return 5;
        if (complexityScore >= 40) return 4;
        if (complexityScore >= 25) return 3;
        if (complexityScore >= 10) return 2;
        return 1;
    }

    public int calculateReadabilityScore(int blocks, int sprites, int vars) {
        if (blocks == 0) return 1;
        // 每个角色平均积木数
        double blocksPerSprite = sprites > 0 ? (double) blocks / sprites : blocks;
        // 适中为好：10-50 块/角色
        int score;
        if (blocksPerSprite >= 10 && blocksPerSprite <= 50) score = 4;
        else if (blocksPerSprite >= 5 && blocksPerSprite <= 100) score = 3;
        else score = 2;
        // 有变量加分
        if (vars > 0) score++;
        return Math.min(score, 5);
    }

    public int calculateBestPracticeScore(int blocks, int events, int control, int sprites) {
        if (blocks == 0) return 1;
        int score = 2; // 基础分
        // 有事件驱动
        if (events > 0) score++;
        // 有控制流
        if (control > 0) score++;
        // 多角色协作（广播）
        if (sprites > 1 && events > 1) score++;
        return Math.min(score, 5);
    }

    // ===== 评语生成 =====

    public String generateSummary(int overall, Map<String, Integer> dims) {
        return switch (overall) {
            case 5 -> "🌟 出色的作品！代码结构清晰，创意十足，展现了扎实的编程功底。";
            case 4 -> "👍 优秀的作品！代码质量良好，有一定的复杂度和创意表现。";
            case 3 -> "✅ 不错的作品！基础功能完成良好，可以尝试添加更多互动元素。";
            case 2 -> "💪 继续加油！作品有基础框架，建议多使用控制流和变量来丰富功能。";
            default -> "🌱 刚起步的作品，建议多学习事件驱动和循环结构，让作品更有趣。";
        };
    }

    public String generateDetail(int overall, Map<String, Integer> dims, int blocks, int sprites, double complexity) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 📊 项目分析报告\n\n");
        sb.append(String.format("- **积木总数**: %d 块\n", blocks));
        sb.append(String.format("- **角色数量**: %d 个\n", sprites));
        sb.append(String.format("- **复杂度评分**: %.1f / 100\n\n", complexity));

        sb.append("## 📈 各维度评分\n\n");
        Map<String, String> dimNames = Map.of(
                "codeStructure", "代码结构",
                "creativity", "创意表现",
                "complexity", "复杂度",
                "readability", "可读性",
                "bestPractice", "最佳实践"
        );
        for (Map.Entry<String, Integer> entry : dims.entrySet()) {
            String name = dimNames.getOrDefault(entry.getKey(), entry.getKey());
            int score = entry.getValue();
            String bar = "█".repeat(score) + "░".repeat(5 - score);
            sb.append(String.format("- %s: %s %d/5\n", name, bar, score));
        }

        sb.append("\n## 💡 点评总结\n\n");
        sb.append(generateSummary(overall, dims));

        return sb.toString();
    }

    public List<String> generateStrengths(Map<String, Integer> dims, int blocks, int sprites, int vars, int broadcasts) {
        List<String> strengths = new ArrayList<>();

        if (dims.getOrDefault("codeStructure", 0) >= 4)
            strengths.add("代码结构清晰，善于使用控制流和数据结构");
        if (dims.getOrDefault("creativity", 0) >= 4)
            strengths.add("创意丰富，角色设计多样，互动性强");
        if (dims.getOrDefault("complexity", 0) >= 4)
            strengths.add("项目复杂度较高，展现了较强的编程能力");
        if (dims.getOrDefault("bestPractice", 0) >= 4)
            strengths.add("遵循良好的编程实践，代码组织合理");
        if (blocks >= 50)
            strengths.add("代码量充足，功能完整");
        if (sprites >= 3)
            strengths.add("多角色协作，场景丰富");
        if (vars > 0)
            strengths.add("善于使用变量管理状态");
        if (broadcasts > 0)
            strengths.add("使用广播实现角色间通信，这是优秀的编程习惯");

        if (strengths.isEmpty()) {
            strengths.add("完成了基本功能，迈出了编程的第一步");
        }
        return strengths;
    }

    public List<String> generateSuggestions(Map<String, Integer> dims, int blocks, int sprites, int control, int custom) {
        List<String> suggestions = new ArrayList<>();

        if (dims.getOrDefault("codeStructure", 0) <= 2)
            suggestions.add("尝试使用更多「如果...那么」和「重复」积木来组织代码逻辑");
        if (dims.getOrDefault("creativity", 0) <= 2)
            suggestions.add("可以添加更多角色，或使用「广播」让角色之间互动");
        if (dims.getOrDefault("complexity", 0) <= 2)
            suggestions.add("尝试使用变量和列表来管理数据，增加程序的复杂度");
        if (dims.getOrDefault("readability", 0) <= 2)
            suggestions.add("每个角色的积木不宜过多或过少，建议每个角色 10-50 块积木");
        if (dims.getOrDefault("bestPractice", 0) <= 2)
            suggestions.add("多使用「当绿旗被点击」事件启动程序，避免使用「永远」循环阻塞");
        if (control == 0 && blocks > 10)
            suggestions.add("项目还没有使用控制流积木，尝试添加循环和条件判断");
        if (custom == 0 && blocks > 30)
            suggestions.add("代码较多时，可以使用「自定义积木」(函数) 来组织重复逻辑");
        if (sprites <= 1 && blocks > 20)
            suggestions.add("考虑添加更多角色，让作品场景更丰富");

        if (suggestions.isEmpty()) {
            suggestions.add("继续保持！可以尝试学习更高级的编程概念，如克隆、链表等");
        }
        return suggestions;
    }

    // ===== 工具方法 =====

    public int countOpcodes(Map<String, Integer> typeCounts, String prefix) {
        return typeCounts.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }
}
