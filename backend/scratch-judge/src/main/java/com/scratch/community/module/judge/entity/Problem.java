package com.scratch.community.module.judge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 题目实体
 * 对应 database problem 表
 *
 * 注意：options/expectedOutput 是 JSON 字符串，直接存储不经过 JacksonTypeHandler
 * 只有非 String 类型的 JSON 字段才需要 JacksonTypeHandler
 */
@Getter
@Setter
@TableName(value = "problem", autoResultMap = true)
public class Problem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建者 ID（教师/管理员） */
    private Long creatorId;

    /** 题目标题 */
    private String title;

    /** 题目描述（Markdown） */
    private String description;

    /**
     * 题目类型:
     * - scratch_algo: Scratch 编程题（上传 sb3 判题）
     * - choice: 选择题
     * - true_false: 判断题
     */
    private String type;

    /** 难度: easy / medium / hard */
    private String difficulty;

    /** 分类/标签（逗号分隔） */
    private String tags;

    /** 积分 */
    private Integer score;

    /** 选择题/判断题的选项 JSON 字符串: [{"key":"A","text":"..."},...] */
    private String options;

    /** 正确答案（选择题: "A"，判断题: "true"/"false"，编程题: null） */
    private String answer;

    /** Scratch 编程题的预期输出 JSON 字符串（用于判题比对） */
    private String expectedOutput;

    /** Scratch 编程题的 sb3 模板文件 URL */
    private String templateSb3Url;

    /** 状态: draft / published */
    private String status;

    /** 提交次数 */
    private Integer submitCount;

    /** 通过次数 */
    private Integer acceptCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
