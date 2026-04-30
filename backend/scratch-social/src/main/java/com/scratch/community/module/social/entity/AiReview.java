package com.scratch.community.module.social.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 点评记录
 */
@Getter
@Setter
@TableName("ai_review")
public class AiReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目 ID */
    private Long projectId;

    /** 触发用户 ID */
    private Long userId;

    /**
     * 总体评分 (1-5 星)
     */
    private Integer overallScore;

    /**
     * 各维度评分 (JSON):
     * - codeStructure: 代码结构
     * - creativity: 创意
     * - complexity: 复杂度
     * - readability: 可读性
     * - bestPractice: 最佳实践
     */
    private String dimensionScores;

    /** 总结评语 */
    private String summary;

    /** 详细点评 (Markdown) */
    private String detail;

    /** 优点列表 (JSON 数组) */
    private String strengths;

    /** 改进建议 (JSON 数组) */
    private String suggestions;

    /** 分析的积木数 */
    private Integer blockCount;

    /** 分析的角色数 */
    private Integer spriteCount;

    /** 复杂度评分 */
    private Double complexityScore;

    /**
     * 生成方式:
     * - RULE: 规则引擎
     * - LLM: 大语言模型
     */
    private String provider;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
