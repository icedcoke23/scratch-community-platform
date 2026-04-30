package com.scratch.community.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核操作请求
 */
@Data
public class AuditActionDTO {

    /** 操作: passed / rejected */
    @NotBlank(message = "操作类型不能为空")
    private String action;

    /** 拒绝原因（action=rejected 时必填） */
    private String reason;
}
