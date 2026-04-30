package com.scratch.community.module.social.ai;

import java.util.List;
import java.util.Map;

/**
 * AI 点评结果（纯数据，用于方法间传递）
 */
public class ReviewResult {
    public int overallScore;
    public Map<String, Integer> dimensionScores;
    public String summary;
    public String detail;
    public List<String> strengths;
    public List<String> suggestions;
    public int spriteCount;
}
