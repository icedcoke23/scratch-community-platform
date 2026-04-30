package com.scratch.community.module.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 添加评论 DTO
 */
@Data
public class AddCommentDTO {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过 500 字")
    private String content;
}
