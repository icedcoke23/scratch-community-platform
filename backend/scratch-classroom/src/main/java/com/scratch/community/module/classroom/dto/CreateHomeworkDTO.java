package com.scratch.community.module.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建作业 DTO
 */
@Data
public class CreateHomeworkDTO {

    @NotNull(message = "班级 ID 不能为空")
    private Long classId;

    @NotBlank(message = "作业标题不能为空")
    private String title;

    private String description;

    private String type; // scratch_project / choice / mixed

    /** 关联的题目 ID 列表（选择题） */
    private List<Long> problemIds;

    /** 截止时间 */
    private LocalDateTime deadline;

    private Integer totalScore;
}
