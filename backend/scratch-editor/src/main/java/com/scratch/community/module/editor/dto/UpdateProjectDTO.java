package com.scratch.community.module.editor.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新项目 DTO（所有字段可选，支持部分更新）
 */
@Data
public class UpdateProjectDTO {

    @Size(max = 200, message = "标题最多 200 字")
    private String title;

    @Size(max = 5000, message = "描述最多 5000 字")
    private String description;

    private String coverUrl;

    private String tags;
}
