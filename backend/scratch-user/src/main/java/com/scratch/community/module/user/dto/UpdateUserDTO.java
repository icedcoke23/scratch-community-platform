package com.scratch.community.module.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人信息请求
 */
@Data
public class UpdateUserDTO {

    @Size(max = 50, message = "昵称最长 50")
    private String nickname;

    @Size(max = 500, message = "简介最长 500")
    private String bio;
}
