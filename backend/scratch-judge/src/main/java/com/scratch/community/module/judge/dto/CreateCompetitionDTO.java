package com.scratch.community.module.judge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建竞赛 DTO
 */
@Data
public class CreateCompetitionDTO {

    @NotBlank(message = "竞赛标题不能为空")
    private String title;

    private String description;

    /** 竞赛类型: TIMED/RATED */
    private String type;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 题目 ID 列表 */
    @NotNull(message = "题目列表不能为空")
    private List<Long> problemIds;

    /** 每题分值（可选，默认每题 100 分） */
    private List<Integer> problemScores;

    /** 是否公开 */
    private Boolean isPublic;
}
