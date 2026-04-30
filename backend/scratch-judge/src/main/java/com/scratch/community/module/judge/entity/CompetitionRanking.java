package com.scratch.community.module.judge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 竞赛排名记录
 */
@Getter
@Setter
@TableName("competition_ranking")
public class CompetitionRanking {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 竞赛 ID */
    private Long competitionId;

    /** 用户 ID */
    private Long userId;

    /** 总得分 */
    private Integer totalScore;

    /** 通过题目数 */
    private Integer solvedCount;

    /** 总罚时（分钟） */
    private Integer penalty;

    /** 排名 */
    private Integer rank;

    /** 各题得分详情 (JSON) */
    private String problemDetails;

    /** 最后提交时间 */
    private LocalDateTime lastSubmitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
