package com.scratch.community.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核记录 VO
 */
@Data
public class AuditLogVO {

    private Long id;
    private String contentType;
    private Long contentId;
    private String contentText;
    private String status;
    private String reason;
    private Long operatorId;
    private LocalDateTime createdAt;
}
