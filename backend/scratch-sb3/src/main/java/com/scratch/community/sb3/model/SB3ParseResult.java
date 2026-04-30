package com.scratch.community.sb3.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * sb3 解析结果
 */
@Data
public class SB3ParseResult {

    /** 项目名称 */
    private String projectName;

    /** 角色数（不含舞台） */
    private int spriteCount;

    /** 总积木数 */
    private int blockCount;

    /** 变量数 */
    private int variableCount;

    /** 列表数 */
    private int listCount;

    /** 广播数 */
    private int broadcastCount;

    /** 复杂度评分 0-100 */
    private double complexityScore;

    /** 角色信息列表 */
    private List<SpriteInfo> sprites;

    /** 各类型积木统计 (opcode → count) */
    private Map<String, Integer> blockTypeCounts;
}
