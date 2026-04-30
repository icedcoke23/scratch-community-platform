package com.scratch.community.sb3.parser;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * 复杂度评分计算器
 * 基于积木类型权重 + 嵌套深度 + 控制流数量，计算 0-100 分
 *
 * 评分公式:
 *   score = baseScore + nestingBonus + controlFlowBonus
 *   其中:
 *     baseScore = Σ(每个积木的权重) / maxBlocks * 60
 *     nestingBonus = maxNestingDepth / 10 * 20
 *     controlFlowBonus = controlFlowCount / totalBlocks * 20
 *   最终 clamp 到 0-100
 */
public class ComplexityCalculator {

    // 积木类型权重（opcode 前缀 → 权重）
    private static final Map<String, Integer> CATEGORY_WEIGHTS = Map.ofEntries(
            Map.entry("motion", 1),
            Map.entry("looks", 1),
            Map.entry("sound", 1),
            Map.entry("event", 1),
            Map.entry("control", 3),
            Map.entry("sensing", 2),
            Map.entry("operator", 2),
            Map.entry("data", 1),
            Map.entry("procedures", 5),
            Map.entry("pen", 2),
            Map.entry("music", 4),
            Map.entry("video", 4),
            Map.entry("extension", 3)
    );

    // 控制流相关 opcode
    private static final String[] CONTROL_FLOW_OPCODES = {
            "control_repeat",
            "control_forever",
            "control_if",
            "control_if_else",
            "control_wait_until",
            "control_repeat_until",
            "control_while",
            "control_for_each"
    };

    // SUBSTACK 输入名（只有这些才是真正的子块链，会增加嵌套深度）
    private static final String[] SUBSTACK_INPUTS = {"SUBSTACK", "SUBSTACK2"};

    /**
     * 计算复杂度评分
     * @param allBlocks 所有积木
     * @param totalBlockCount 总积木数
     * @return 0-100 的评分
     */
    public double calculate(Map<String, JsonNode> allBlocks, int totalBlockCount) {
        if (totalBlockCount == 0 || allBlocks.isEmpty()) {
            return 0.0;
        }

        // 1. 基础分：加权积木数
        double weightedSum = 0;
        for (JsonNode block : allBlocks.values()) {
            if (block == null || block.isNull()) continue;
            JsonNode opcodeNode = block.get("opcode");
            if (opcodeNode == null || opcodeNode.isNull()) continue;
            weightedSum += getCategoryWeight(opcodeNode.asText());
        }
        double baseScore = Math.min(weightedSum / (5.0 * 200) * 60, 60);

        // 2. 嵌套深度加分（最多 20 分）
        int maxNesting = calculateMaxNesting(allBlocks);
        double nestingBonus = Math.min((double) maxNesting / 10 * 20, 20);

        // 3. 控制流加分（最多 20 分）
        int controlFlowCount = countControlFlow(allBlocks);
        double controlFlowBonus = Math.min((double) controlFlowCount / totalBlockCount * 20 * 5, 20);

        double score = baseScore + nestingBonus + controlFlowBonus;
        return Math.round(Math.max(0, Math.min(100, score)) * 10.0) / 10.0;
    }

    /**
     * 获取积木类别的权重
     */
    private int getCategoryWeight(String opcode) {
        String category = opcode.contains("_") ? opcode.substring(0, opcode.indexOf('_')) : opcode;
        return CATEGORY_WEIGHTS.getOrDefault(category, 2);
    }

    /**
     * 计算最大嵌套深度
     */
    private int calculateMaxNesting(Map<String, JsonNode> allBlocks) {
        int maxDepth = 0;

        for (Map.Entry<String, JsonNode> entry : allBlocks.entrySet()) {
            JsonNode block = entry.getValue();
            if (block == null || block.isNull()) continue;

            // 只从顶层积木开始追踪
            JsonNode topLevelNode = block.get("topLevel");
            if (topLevelNode != null && topLevelNode.asBoolean(false)) {
                int depth = calculateChainDepth(entry.getKey(), allBlocks, 0);
                maxDepth = Math.max(maxDepth, depth);
            }
        }

        return maxDepth;
    }

    /**
     * 计算单条链的嵌套深度
     * 只追踪 SUBSTACK/SUBSTACK2 输入和 next 链
     */
    private int calculateChainDepth(String blockId, Map<String, JsonNode> allBlocks, int currentDepth) {
        if (currentDepth > 50) return currentDepth; // 防止无限递归

        JsonNode block = allBlocks.get(blockId);
        if (block == null || block.isNull()) return currentDepth;

        int maxChildDepth = currentDepth;

        // 只检查 SUBSTACK / SUBSTACK2 输入（if/else 有两个子链）
        // 其他输入（如 operator_add 的 operands）是 reporter，不增加嵌套深度
        JsonNode inputs = block.get("inputs");
        if (inputs != null && inputs.isObject()) {
            for (String substackKey : SUBSTACK_INPUTS) {
                JsonNode value = inputs.get(substackKey);
                if (value != null && value.isArray() && value.size() >= 2) {
                    JsonNode second = value.get(1);
                    if (second != null && second.isTextual()) {
                        String childId = second.asText();
                        int childDepth = calculateChainDepth(childId, allBlocks, currentDepth + 1);
                        maxChildDepth = Math.max(maxChildDepth, childDepth);
                    }
                }
            }
        }

        // 追踪 next 链（顺序执行，不增加深度）
        JsonNode nextNode = block.get("next");
        if (nextNode != null && !nextNode.isNull()) {
            String next = nextNode.asText();
            if (next != null) {
                int nextDepth = calculateChainDepth(next, allBlocks, currentDepth);
                maxChildDepth = Math.max(maxChildDepth, nextDepth);
            }
        }

        return maxChildDepth;
    }

    /**
     * 统计控制流积木数量
     */
    private int countControlFlow(Map<String, JsonNode> allBlocks) {
        int count = 0;
        for (JsonNode block : allBlocks.values()) {
            if (block == null || block.isNull()) continue;
            JsonNode opcodeNode = block.get("opcode");
            if (opcodeNode == null || opcodeNode.isNull()) continue;
            String opcode = opcodeNode.asText();

            for (String cfOpcode : CONTROL_FLOW_OPCODES) {
                if (opcode.startsWith(cfOpcode)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }
}
