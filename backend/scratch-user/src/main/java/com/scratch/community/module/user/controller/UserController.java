package com.scratch.community.module.user.controller;

import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.auth.RequireRole;
import com.scratch.community.common.result.R;
import com.scratch.community.module.user.dto.*;
import com.scratch.community.module.user.service.ClassService;
import com.scratch.community.module.user.service.UserService;
import com.scratch.community.module.user.vo.ClassVO;
import com.scratch.community.module.user.vo.LoginVO;
import com.scratch.community.module.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户模块 API
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ClassService classService;

    // ==================== 认证 ====================

    @PostMapping("/user/register")
    public R<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(userService.register(dto));
    }

    @PostMapping("/user/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(userService.login(dto));
    }

    // ==================== 个人信息 ====================

    @GetMapping("/user/me")
    public R<UserVO> me() {
        return R.ok(userService.getUserInfo(LoginUser.getUserId()));
    }

    @PutMapping("/user/me")
    public R<Void> updateMe(@Valid @RequestBody UpdateUserDTO dto) {
        userService.updateUser(LoginUser.getUserId(), dto);
        return R.ok();
    }

    @PutMapping("/user/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(LoginUser.getUserId(), dto);
        return R.ok();
    }

    @PostMapping("/user/avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return R.ok(userService.uploadAvatar(LoginUser.getUserId(), file));
    }

    @GetMapping("/user/{id}/profile")
    public R<UserVO> userProfile(@PathVariable Long id) {
        return R.ok(userService.getUserInfo(id));
    }

    // ==================== 关注 ====================

    @PostMapping("/user/{id}/follow")
    public R<Void> follow(@PathVariable Long id) {
        userService.follow(LoginUser.getUserId(), id);
        return R.ok();
    }

    @DeleteMapping("/user/{id}/follow")
    public R<Void> unfollow(@PathVariable Long id) {
        userService.unfollow(LoginUser.getUserId(), id);
        return R.ok();
    }

    // ==================== 搜索 ====================

    @GetMapping("/user/search")
    public R<?> searchUsers(@RequestParam String q,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size) {
        return R.ok(userService.searchUsers(q, new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size)));
    }

    // ==================== 班级 ====================

    @PostMapping("/class")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<ClassVO> createClass(@Valid @RequestBody CreateClassDTO dto) {
        return R.ok(classService.createClass(LoginUser.getUserId(), dto));
    }

    @GetMapping("/class")
    public R<?> myClasses() {
        return R.ok(classService.getMyClasses(LoginUser.getUserId()));
    }

    @GetMapping("/class/{id}")
    public R<ClassVO> classDetail(@PathVariable Long id) {
        return R.ok(classService.getClassDetail(id));
    }

    @PostMapping("/class/{id}/join")
    public R<Void> joinClass(@PathVariable Long id, @RequestParam String inviteCode) {
        classService.joinClass(LoginUser.getUserId(), inviteCode);
        return R.ok();
    }

    @DeleteMapping("/class/{id}/member/{uid}")
    @RequireRole({"TEACHER", "ADMIN"})
    public R<Void> removeMember(@PathVariable Long id, @PathVariable Long uid) {
        classService.removeMember(id, LoginUser.getUserId(), uid);
        return R.ok();
    }

    @GetMapping("/class/{id}/members")
    public R<?> classMembers(@PathVariable Long id) {
        return R.ok(classService.getClassMembers(id));
    }
}
