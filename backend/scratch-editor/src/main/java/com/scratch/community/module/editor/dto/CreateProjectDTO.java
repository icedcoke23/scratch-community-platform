package com.scratch.community.module.editor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建项目 DTO
 */
@Data
public class CreateProjectDTO {

    @NotBlank(message = "项目标题不能为空")
    @Size(max = 200, message = "标题最多 200 字")
    private String title;

    @Size(max = 5000, message = "描述最多 5000 字")
    private String description;

    /** 封面 URL */
    private String coverUrl;

    /** 标签 (逗号分隔) */
    private String tags;
}
