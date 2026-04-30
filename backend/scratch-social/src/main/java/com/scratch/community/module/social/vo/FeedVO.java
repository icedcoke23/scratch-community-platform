package com.scratch.community.module.social.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区项目列表 VO（用于 Feed 流）
 */
@Data
public class FeedVO {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String coverUrl;
    private String tags;
    private Integer blockCount;
    private Double complexityScore;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private LocalDateTime createdAt;

    // 用户信息
    private String username;
    private String nickname;
    private String avatarUrl;

    // 当前用户状态
    private Boolean isLiked;
}
