package com.scratch.community.module.editor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目列表 VO（脱敏，不含 sb3Url/parseResult）
 */
@Data
public class ProjectVO {

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
}
