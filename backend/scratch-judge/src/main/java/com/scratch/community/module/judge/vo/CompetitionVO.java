package com.scratch.community.module.judge.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞赛列表 VO
 */
@Data
public class CompetitionVO {
    private Long id;
    private String title;
    private String description;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalScore;
    private Integer participantCount;
    private String status;
    private Boolean isPublic;
    private LocalDateTime createdAt;

    /** 题目数量 */
    private Integer problemCount;

    /** 当前用户是否已报名 */
    private Boolean registered;

    /** 剩余时间（秒），进行中时有效 */
    private Long remainingSeconds;
}
