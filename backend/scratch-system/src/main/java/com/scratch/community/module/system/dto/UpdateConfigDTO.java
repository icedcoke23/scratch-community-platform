package com.scratch.community.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新配置请求
 */
@Data
public class UpdateConfigDTO {

    @NotBlank(message = "配置值不能为空")
    private String configValue;

    private String description;
}
