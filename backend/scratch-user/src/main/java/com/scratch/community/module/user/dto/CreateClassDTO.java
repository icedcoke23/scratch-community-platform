package com.scratch.community.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建班级请求
 */
@Data
public class CreateClassDTO {

    @NotBlank(message = "班级名称不能为空")
    @Size(max = 100, message = "名称最长 100")
    private String name;

    @Size(max = 50, message = "年级最长 50")
    private String grade;

    @NotBlank(message = "邀请码不能为空")
    @Size(min = 4, max = 20, message = "邀请码长度 4-20")
    private String inviteCode;
}
