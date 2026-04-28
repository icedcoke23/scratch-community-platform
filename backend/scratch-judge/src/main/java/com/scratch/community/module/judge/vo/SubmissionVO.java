package com.scratch.community.module.judge.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交记录 VO
 */
@Data
public class SubmissionVO {
    private Long id;
    private Long problemId;
    private String problemTitle;
    private String submitType;
    private String answer;
    private String verdict;
    private String judgeDetail;
    private Long runtimeMs;
    private Long memoryKb;
    private LocalDateTime createdAt;
}
