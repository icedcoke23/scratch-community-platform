package com.scratch.community.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度 3-50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度 8-50")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>/?]).+$",
             message = "密码必须包含字母、数字和特殊字符")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最长 50")
    private String nickname;

    /**
     * 邮箱（可选，用于密码找回）
     */
    @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 角色: STUDENT / TEACHER
     */
    private String role = "STUDENT";
}
