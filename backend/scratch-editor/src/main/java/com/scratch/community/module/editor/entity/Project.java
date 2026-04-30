package com.scratch.community.module.editor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 项目实体
 * 对应数据库 project 表
 */
@Getter
@Setter
@TableName(value = "project", autoResultMap = true)
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 作者 ID */
    private Long userId;

    /** 项目标题 */
    private String title;

    /** 项目描述 */
    private String description;

    /** 封面 URL */
    private String coverUrl;

    /** sb3 文件 URL */
    private String sb3Url;

    /** 状态: draft / published */
    private String status;

    /** 积木数量 */
    private Integer blockCount;

    /** 复杂度评分 */
    private Double complexityScore;

    /** 点赞数 */
    private Integer likeCount;

    /** 评论数 */
    private Integer commentCount;

    /** 浏览数 */
    private Integer viewCount;

    /** sb3 解析结果 (JSON) */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String parseResult;

    /** 标签 (逗号分隔) */
    private String tags;

    /** 原始项目 ID（Remix 来源，null 表示原创） */
    private Long remixProjectId;

    /** Remix 次数 */
    private Integer remixCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
