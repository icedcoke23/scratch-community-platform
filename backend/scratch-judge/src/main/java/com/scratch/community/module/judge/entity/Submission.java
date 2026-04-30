package com.scratch.community.module.judge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 提交记录实体
 * 对应 database submission 表
 *
 * 注意：judgeDetail 是 JSON 字符串，直接存储不经过 JacksonTypeHandler
 */
@Getter
@Setter
@TableName(value = "submission", autoResultMap = true)
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 题目 ID */
    private Long problemId;

    /**
     * 提交类型:
     * - sb3: Scratch 项目文件
     * - choice: 选择题答案
     * - true_false: 判断题答案
     */
    private String submitType;

    /** 提交内容（选择题/判断题的答案，编程题为 null） */
    private String answer;

    /** 提交的 sb3 文件 URL（编程题） */
    private String sb3Url;

    /**
     * 判题结果:
     * - PENDING: 待判题
     * - AC: 通过 (Accepted)
     * - WA: 答案错误 (Wrong Answer)
     * - TLE: 超时 (Time Limit Exceeded)
     * - RE: 运行错误 (Runtime Error)
     * - CE: 编译错误 (Compile Error)
     */
    private String verdict;

    /** 判题详情 JSON 字符串（错误信息、输出比对等） */
    private String judgeDetail;

    /** 运行耗时（ms） */
    private Long runtimeMs;

    /** 运行内存（KB） */
    private Long memoryKb;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
