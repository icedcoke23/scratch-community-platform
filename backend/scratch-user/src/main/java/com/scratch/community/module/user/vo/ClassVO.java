package com.scratch.community.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级 VO
 */
@Data
public class ClassVO {

    private Long id;
    private String name;
    private String grade;
    private String inviteCode;
    private Long teacherId;
    private String teacherName;
    private Integer studentCount;
    private LocalDateTime createdAt;
}
