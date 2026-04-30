package com.scratch.community.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知 VO
 */
@Data
public class NotificationVO {

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String data;
    private Integer isRead;
    private LocalDateTime createdAt;
}
