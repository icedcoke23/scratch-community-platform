package com.scratch.community.module.editor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目详情 VO（包含完整信息）
 * 不继承 ProjectVO，避免 Lombok @Data 继承 equals/hashCode 对称性问题
 */
@Data
public class ProjectDetailVO {

    private Long id;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String title;
    private String description;
    private String coverUrl;
    private String status;
    private Integer blockCount;
    private Double complexityScore;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private String tags;
    private LocalDateTime createdAt;

    // 详情页额外字段
    /** sb3 文件下载链接 */
    private String sb3Url;
    /** sb3 解析结果 JSON */
    private String parseResult;
    /** 当前用户是否已点赞 */
    private Boolean isLiked;
}
