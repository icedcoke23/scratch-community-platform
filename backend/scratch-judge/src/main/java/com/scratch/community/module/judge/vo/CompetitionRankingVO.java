package com.scratch.community.module.judge.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞赛排名 VO
 */
@Data
public class CompetitionRankingVO {
    private Long id;
    private Long competitionId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Integer totalScore;
    private Integer solvedCount;
    private Integer penalty;
    private Integer rank;
    private String problemDetails;
    private LocalDateTime lastSubmitTime;
}
