package com.scratch.community.module.social.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 点评 VO
 */
@Data
public class AiReviewVO {

    private Long id;
    private Long projectId;
    private Long userId;

    /** 总体评分 (1-5) */
    private Integer overallScore;

    /** 各维度评分 */
    private Map<String, Integer> dimensionScores;

    /** 总结评语 */
    private String summary;

    /** 详细点评 (Markdown) */
    private String detail;

    /** 优点 */
    private List<String> strengths;

    /** 改进建议 */
    private List<String> suggestions;

    /** 项目统计 */
    private Integer blockCount;
    private Integer spriteCount;
    private Double complexityScore;

    /** 生成方式 */
    private String provider;

    private LocalDateTime createdAt;

    /** 评分星级显示 */
    public String getStarDisplay() {
        if (overallScore == null) return "☆☆☆☆☆";
        return "★".repeat(overallScore) + "☆".repeat(5 - overallScore);
    }
}
