package com.scratch.community.module.judge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 竞赛报名记录
 */
@Getter
@Setter
@TableName("competition_registration")
public class CompetitionRegistration {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 竞赛 ID */
    private Long competitionId;

    /** 用户 ID */
    private Long userId;

    /** 报名时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
