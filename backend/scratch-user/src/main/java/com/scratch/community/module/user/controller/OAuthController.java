package com.scratch.community.module.user.controller;

import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.result.R;
import com.scratch.community.module.user.dto.OAuthLoginDTO;
import com.scratch.community.module.user.entity.UserOAuth;
import com.scratch.community.module.user.service.OAuthService;
import com.scratch.community.module.user.vo.OAuthCallbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 第三方登录 API
 */
@Tag(name = "第三方登录", description = "微信/QQ 等第三方平台登录")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    // ==================== 登录 ====================

    @Operation(summary = "第三方登录",
            description = "使用微信/QQ等第三方平台授权码登录。新用户会自动注册。")
    @PostMapping("/oauth/login")
    public R<OAuthCallbackVO> oauthLogin(@Valid @RequestBody OAuthLoginDTO dto) {
        return R.ok(oAuthService.oauthLogin(dto));
    }

    // ==================== 绑定管理 ====================

    @Operation(summary = "绑定第三方账号",
            description = "将第三方平台账号绑定到当前已登录用户")
    @PostMapping("/oauth/bind")
    public R<Void> bindOAuth(@Valid @RequestBody OAuthLoginDTO dto) {
        oAuthService.bindOAuth(LoginUser.getUserId(), dto);
        return R.ok();
    }

    @Operation(summary = "解绑第三方账号",
            description = "解除当前用户与指定第三方平台的绑定关系")
    @DeleteMapping("/oauth/bind/{provider}")
    public R<Void> unbindOAuth(@PathVariable String provider) {
        oAuthService.unbindOAuth(LoginUser.getUserId(), provider);
        return R.ok();
    }

    @Operation(summary = "查看绑定列表",
            description = "获取当前用户已绑定的所有第三方平台")
    @GetMapping("/oauth/bindings")
    public R<List<UserOAuth>> getBindings() {
        return R.ok(oAuthService.getBindings(LoginUser.getUserId()));
    }
}
