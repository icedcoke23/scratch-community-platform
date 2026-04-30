package com.scratch.community.sb3.parser;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

/**
 * 积木统计器
 * 统计所有积木的数量和各类型（opcode）的分布
 */
public class BlockCounter {

    /**
     * 统计结果
     */
    public static class CountResult {
        /** 总积木数（非 shadow/非变量引用） */
        private final int totalBlocks;
        /** 各 opcode 数量 */
        private final Map<String, Integer> typeCounts;

        public CountResult(int totalBlocks, Map<String, Integer> typeCounts) {
            this.totalBlocks = totalBlocks;
            this.typeCounts = typeCounts;
        }

        public int getTotalBlocks() { return totalBlocks; }
        public Map<String, Integer> getTypeCounts() { return typeCounts; }
    }

    /**
     * 统计积木
     * @param allBlocks 所有积木 (blockId → block 对象)
     * @return 统计结果
     */
    public CountResult count(Map<String, JsonNode> allBlocks) {
        Map<String, Integer> typeCounts = new HashMap<>();
        int realBlockCount = 0;

        for (JsonNode block : allBlocks.values()) {
            if (block == null || block.isNull()) continue;

            JsonNode opcodeNode = block.get("opcode");
            if (opcodeNode == null || opcodeNode.isNull()) continue;
            String opcode = opcodeNode.asText();

            // 统计所有 opcode 的分布
            typeCounts.merge(opcode, 1, Integer::sum);

            // 统计"真实"积木：
            // - shadow 字段为 null 或 "no_shadow" 的积木
            // - 或者是 topLevel 积木
            // 排除 shadow="shadow" 且非 topLevel 的占位块
            JsonNode shadowNode = block.get("shadow");
            String shadow = shadowNode != null && !shadowNode.isNull() ? shadowNode.asText() : null;
            boolean isShadow = "shadow".equals(shadow);

            JsonNode topLevelNode = block.get("topLevel");
            boolean isTopLevel = topLevelNode != null && topLevelNode.asBoolean(false);

            if (!isShadow || isTopLevel) {
                realBlockCount++;
            }
        }

        return new CountResult(realBlockCount, typeCounts);
    }
}
