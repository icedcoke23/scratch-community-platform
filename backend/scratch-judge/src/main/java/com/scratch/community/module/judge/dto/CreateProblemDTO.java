package com.scratch.community.module.judge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建题目 DTO
 */
@Data
public class CreateProblemDTO {

    @NotBlank(message = "题目标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "题目类型不能为空")
    private String type; // scratch_algo / choice / true_false

    private String difficulty; // easy / medium / hard

    private String tags;

    private Integer score;

    /** 选择题选项 */
    private List<OptionItem> options;

    /** 正确答案 */
    private String answer;

    /** 编程题预期输出 */
    private String expectedOutput;

    @Data
    public static class OptionItem {
        private String key;   // A, B, C, D
        private String text;  // 选项文本
    }
}
