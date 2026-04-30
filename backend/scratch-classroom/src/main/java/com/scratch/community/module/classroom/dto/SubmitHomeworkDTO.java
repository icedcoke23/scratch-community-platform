package com.scratch.community.module.classroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交作业 DTO
 */
@Data
public class SubmitHomeworkDTO {

    @NotNull(message = "作业 ID 不能为空")
    private Long homeworkId;

    /** 提交的项目 ID（Scratch 项目作业） */
    private Long projectId;

    /** 提交的答案 JSON（选择题作业） */
    private String answers;
}
