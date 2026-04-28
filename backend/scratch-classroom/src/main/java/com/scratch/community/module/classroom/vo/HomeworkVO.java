package com.scratch.community.module.classroom.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业列表 VO
 */
@Data
public class HomeworkVO {
    private Long id;
    private Long classId;
    private String className;
    private String title;
    private String type;
    private LocalDateTime deadline;
    private Integer totalScore;
    private String status;
    private Integer submitCount;
    private Integer gradedCount;
    private LocalDateTime createdAt;
}
