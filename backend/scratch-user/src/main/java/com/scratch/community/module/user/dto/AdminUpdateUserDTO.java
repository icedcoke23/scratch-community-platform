package com.scratch.community.module.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理员更新用户 DTO
 */
@Data
public class AdminUpdateUserDTO {

    /** 角色: STUDENT / TEACHER / ADMIN */
    @Pattern(regexp = "^(STUDENT|TEACHER|ADMIN)$", message = "角色必须为 STUDENT/TEACHER/ADMIN")
    private String role;

    /** 状态: 0=禁用 1=正常 */
    @Pattern(regexp = "^[01]$", message = "状态必须为 0 或 1")
    private String status;
}
