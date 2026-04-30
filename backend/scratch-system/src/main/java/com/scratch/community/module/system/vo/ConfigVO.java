package com.scratch.community.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置 VO
 */
@Data
public class ConfigVO {

    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime updatedAt;
}
