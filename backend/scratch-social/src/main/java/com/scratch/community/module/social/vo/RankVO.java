package com.scratch.community.module.social.vo;

import lombok.Data;

/**
 * 排行榜条目 VO
 */
@Data
public class RankVO {
    private Long rank;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Integer likeCount;
    private Integer projectCount;
}
