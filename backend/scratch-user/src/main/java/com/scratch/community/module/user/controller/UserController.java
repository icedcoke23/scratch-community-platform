package com.scratch.community.module.user.controller;

import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.auth.RequireRole;
import com.scratch.community.common.auth.TokenBlacklistService;
import com.scratch.community.common.auth.JwtUtils;
import com.scratch.community.common.result.R;
import com.scratch.community.module.user.dto.ChangePasswordDTO;
import com.scratch.community.module.user.dto.CreateClassDTO;
import com.scratch.community.module.user.dto.LoginDTO;
import com.scratch.community.module.user.dto.RegisterDTO;
import com.scratch.community.module.user.dto.UpdateUserDTO;
import com.scratch.community.module.user.service.ClassService;
import com.scratch.community.module.user.service.UserService;
import com.scratch.community.module.user.vo.ClassVO;
import com.scratch.community.module.user.vo.LoginVO;
import com.scratch.community.module.user.vo.UserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户模块 API
 */
@Tag(name = "用户", description = "注册/登录/关注/班级")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ClassService classService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtils jwtUtils;

    // ==================== 认证 ====================

    @Operation(summary = "用户注册")
    @PostMapping("/user/register")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(userService.register(dto));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/user/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(userService.login(dto));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/user/logout")
    public R<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 计算 Token 剩余有效期，设置为黑名单 TTL
                var claims = jwtUtils.parseToken(token);
                long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (remainingMs > 0) {
                    tokenBlacklistService.blacklist(token, remainingMs);
                }
                // 清除用户的 Refresh Token
                Long userId = Long.parseLong(claims.getSubject());
                userService.clearRefreshToken(userId);
            } catch (Exception e) {
                // Token 已过期或无效，忽略
            }
        }
        return R.ok();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/user/refresh")
    public R<LoginVO> refresh(@RequestBody java.util.Map<String, String> body) {
        String refreshTokenValue = body.get("refreshToken");
        if (refreshTokenValue == null || refreshTokenValue.trim().isEmpty()) {
            return R.fail(com.scratch.community.common.result.ErrorCode.UNAUTHORIZED);
        }

        // 验证 Refresh Token
        if (!jwtUtils.validateRefreshToken(refreshTokenValue)) {
            return R.fail(com.scratch.community.common.result.ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 检查 Refresh Token 是否已被撤销（黑名单）
        if (tokenBlacklistService.isBlacklisted(refreshTokenValue)) {
            return R.fail(com.scratch.community.common.result.ErrorCode.REFRESH_TOKEN_INVALID);
        }

        try {
            Long userId = jwtUtils.getUserIdFromRefreshToken(refreshTokenValue);
            var user = userService.getUserById(userId);
            if (user == null) {
                return R.fail(com.scratch.community.common.result.ErrorCode.USER_NOT_FOUND);
            }

            // 检查 Refresh Token 是否与存储的一致（防止重放攻击）
            if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshTokenValue)) {
                return R.fail(com.scratch.community.common.result.ErrorCode.REFRESH_TOKEN_INVALID);
            }

            // 检查 Refresh Token 是否过期
            if (user.getRefreshTokenExpiresAt() != null &&
                user.getRefreshTokenExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                return R.fail(com.scratch.community.common.result.ErrorCode.REFRESH_TOKEN_EXPIRED);
            }

            // 🔒 将旧 Refresh Token 加入黑名单（一次性使用）
            long remaining = jwtUtils.getRemainingExpiry(refreshTokenValue);
            tokenBlacklistService.blacklist(refreshTokenValue, remaining);

            // 生成新的 Access Token
            String newAccessToken = jwtUtils.generateToken(userId, user.getUsername(), user.getRole());

            // 检查 Refresh Token 是否快过期（< 1 天），如果是则同时刷新
            String newRefreshToken = refreshTokenValue;
            boolean refreshExpiresSoon = user.getRefreshTokenExpiresAt() != null &&
                user.getRefreshTokenExpiresAt().isBefore(java.time.LocalDateTime.now().plusDays(1));
            if (refreshExpiresSoon) {
                newRefreshToken = jwtUtils.generateRefreshToken(userId);
                userService.updateRefreshToken(userId, newRefreshToken,
                    jwtUtils.getRefreshTokenExpiry(newRefreshToken));
            }

            LoginVO loginVO = new LoginVO();
            loginVO.setToken(newAccessToken);
            loginVO.setRefreshToken(newRefreshToken);
            loginVO.setUserInfo(userService.getUserInfo(userId));
            return R.ok(loginVO);
        } catch (Exception e) {
            return R.fail(com.scratch.community.common.result.ErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    // ==================== 个人信息 ====================

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user/me")
    public R<UserVO> me() {
        return R.ok(userService.getUserInfo(LoginUser.getUserId()));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/user/me")
    public R<Void> updateMe(@Valid @RequestBody UpdateUserDTO dto) {
        userService.updateUser(LoginUser.getUserId(), dto);
        return R.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/user/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(LoginUser.getUserId(), dto);
        return R.ok();
    }

    @Operation(summary = "上传头像")
    @PostMapping("/user/avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return R.ok(userService.uploadAvatar(LoginUser.getUserId(), file));
    }

    @Operation(summary = "查看用户主页")
    @GetMapping("/user/{id}/profile")
    public R<UserVO> userProfile(@PathVariable Long id) {
        return R.ok(userService.getUserInfo(id));
    }

    // ==================== 关注 ====================

    @Operation(summary = "关注用户")
    @PostMapping("/user/{id}/follow")
    public R<Void> follow(@PathVariable Long id) {
        userService.follow(LoginUser.getUserId(), id);
        return R.ok();
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/user/{id}/follow")
    public R<Void> unfollow(@PathVariable Long id) {
        userService.unfollow(LoginUser.getUserId(), id);
        return R.ok();
    }

    // ==================== 搜索 ====================

    @Operation(summary = "搜索用户")
    @GetMapping("/user/search")
    public R<?> searchUsers(@RequestParam String q,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(userService.searchUsers(q, new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size)));
    }

    // ==================== 班级 ====================

    @Operation(summary = "创建班级")
    @PostMapping("/class")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<ClassVO> createClass(@Valid @RequestBody CreateClassDTO dto) {
        return R.ok(classService.create(LoginUser.getUserId(), dto));
    }

    @Operation(summary = "我的班级列表")
    @GetMapping("/class")
    public R<?> myClasses() {
        return R.ok(classService.getMyClasses(LoginUser.getUserId()));
    }

    @Operation(summary = "班级详情")
    @GetMapping("/class/{id}")
    public R<ClassVO> classDetail(@PathVariable Long id) {
        return R.ok(classService.getClassDetail(id));
    }

    @Operation(summary = "加入班级")
    @PostMapping("/class/{id}/join")
    public R<Void> joinClass(@PathVariable Long id, @RequestParam String inviteCode) {
        classService.joinClass(LoginUser.getUserId(), inviteCode);
        return R.ok();
    }

    @Operation(summary = "移除班级成员")
    @DeleteMapping("/class/{id}/member/{uid}")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> removeMember(@PathVariable Long id, @PathVariable Long uid) {
        classService.removeMember(id, LoginUser.getUserId(), uid);
        return R.ok();
    }

    @Operation(summary = "班级成员列表")
    @GetMapping("/class/{id}/members")
    public R<?> classMembers(@PathVariable Long id) {
        return R.ok(classService.getClassMembers(id));
    }
}
