package com.scratch.community.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 第三方登录请求 DTO
 */
@Data
@Schema(description = "第三方登录请求")
public class OAuthLoginDTO {

    @NotBlank(message = "平台不能为空")
    @Schema(description = "第三方平台", allowableValues = {"wechat", "qq"}, example = "wechat")
    private String provider;

    @NotBlank(message = "授权码不能为空")
    @Schema(description = "第三方平台的授权码 (code)", example = "AUTH_CODE_FROM_OAUTH")
    private String code;

    @Schema(description = "微信小程序的自定义登录态（可选，用于小程序登录）")
    private String encryptedData;

    @Schema(description = "微信小程序的初始向量（可选）")
    private String iv;
}
