package com.scratch.community.module.judge.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目列表 VO
 */
@Data
public class ProblemVO {
    private Long id;
    private String title;
    private String type;
    private String difficulty;
    private String tags;
    private Integer score;
    private Integer submitCount;
    private Integer acceptCount;
    private String status;
    private LocalDateTime createdAt;
}
