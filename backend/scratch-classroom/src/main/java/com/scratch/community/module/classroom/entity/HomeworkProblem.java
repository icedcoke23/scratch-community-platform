package com.scratch.community.module.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("homework_problem")
public class HomeworkProblem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long homeworkId;
    private Long problemId;
    private Integer sortOrder;
    private Integer score;
}
