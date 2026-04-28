package com.scratch.community.module.judge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("competition_problem")
public class CompetitionProblem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long competitionId;
    private Long problemId;
    private Integer sortOrder;
    private Integer score;
}
