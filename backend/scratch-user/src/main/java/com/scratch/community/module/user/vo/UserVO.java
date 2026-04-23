package com.scratch.community.module.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 VO (脱敏)
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String role;
    private LocalDateTime createdAt;

    // 统计 (按需填充)
    private Integer projectCount;
    private Integer followerCount;
    private Integer followingCount;
}
