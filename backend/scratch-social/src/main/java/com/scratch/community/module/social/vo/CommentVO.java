package com.scratch.community.module.social.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论 VO
 */
@Data
public class CommentVO {
    private Long id;
    private Long userId;
    private Long projectId;
    private String content;
    private LocalDateTime createdAt;

    // 用户信息（JOIN 查询）
    private String username;
    private String nickname;
    private String avatarUrl;
}
