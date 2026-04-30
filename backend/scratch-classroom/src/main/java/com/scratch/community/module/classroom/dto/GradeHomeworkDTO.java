package com.scratch.community.module.classroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批改作业 DTO
 */
@Data
public class GradeHomeworkDTO {

    @NotNull(message = "提交记录 ID 不能为空")
    private Long submissionId;

    @NotNull(message = "分数不能为空")
    private Integer score;

    /** 教师评语 */
    private String comment;
}
