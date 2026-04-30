package com.scratch.community.module.judge.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交答案 DTO
 */
@Data
public class SubmitDTO {

    @NotNull(message = "题目 ID 不能为空")
    private Long problemId;

    /** 选择题/判断题的答案 */
    private String answer;
}
