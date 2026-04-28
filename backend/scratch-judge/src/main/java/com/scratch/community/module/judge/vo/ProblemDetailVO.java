package com.scratch.community.module.judge.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 题目详情 VO（含选项和描述）
 */
@Data
public class ProblemDetailVO {
    private Long id;
    private String title;
    private String description;
    private String type;
    private String difficulty;
    private String tags;
    private Integer score;
    private List<Map<String, String>> options; // [{key: "A", text: "..."}]
    private String templateSb3Url;
    private Integer submitCount;
    private Integer acceptCount;
    private LocalDateTime createdAt;
}
