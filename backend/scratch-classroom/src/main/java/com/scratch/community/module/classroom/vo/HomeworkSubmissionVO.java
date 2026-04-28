package com.scratch.community.module.classroom.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业提交记录 VO（含学生信息）
 */
@Data
public class HomeworkSubmissionVO {
    private Long id;
    private Long homeworkId;
    private Long studentId;
    private Long projectId;
    private Integer score;
    private String comment;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime gradedAt;

    // 学生信息
    private String username;
    private String nickname;
    private String avatarUrl;
}
