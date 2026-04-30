package com.scratch.community.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.auth.RequireRole;
import com.scratch.community.common.result.R;
import com.scratch.community.module.user.dto.AdminUpdateUserDTO;
import com.scratch.community.module.user.service.AdminDashboardService;
import com.scratch.community.module.user.service.AdminService;
import com.scratch.community.module.user.vo.DashboardVO;
import com.scratch.community.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员 API
 */
@Tag(name = "管理员", description = "用户管理/数据统计（仅 ADMIN）")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminDashboardService adminDashboardService;

    /**
     * 数据统计面板
     */
    @Operation(summary = "数据统计面板")
    @RequireRole("ADMIN")
    @GetMapping("/dashboard")
    public R<DashboardVO> dashboard() {
        return R.ok(adminDashboardService.getDashboard());
    }

    /**
     * 用户列表（分页）
     */
    @Operation(summary = "用户列表")
    @RequireRole("ADMIN")
    @GetMapping("/user")
    public R<?> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(adminService.listUsers(keyword, role, new Page<>(page, size)));
    }

    /**
     * 更新用户（角色/状态）
     */
    @Operation(summary = "更新用户角色/状态")
    @RequireRole("ADMIN")
    @PutMapping("/user/{id}")
    public R<Void> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserDTO dto) {
        adminService.updateUser(id, dto);
        return R.ok();
    }

    /**
     * 禁用用户
     */
    @Operation(summary = "禁用用户")
    @RequireRole("ADMIN")
    @PostMapping("/user/{id}/disable")
    public R<Void> disableUser(@PathVariable Long id) {
        adminService.disableUser(LoginUser.getUserId(), id);
        return R.ok();
    }

    /**
     * 启用用户
     */
    @Operation(summary = "启用用户")
    @RequireRole("ADMIN")
    @PostMapping("/user/{id}/enable")
    public R<Void> enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return R.ok();
    }

    // ==================== 作品管理 ====================

    /**
     * 作品列表（分页）
     */
    @Operation(summary = "作品列表")
    @RequireRole("ADMIN")
    @GetMapping("/project")
    public R<?> listProjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(adminService.listProjects(keyword, status, page, size));
    }

    /**
     * 作品统计
     */
    @Operation(summary = "作品统计")
    @RequireRole("ADMIN")
    @GetMapping("/project/stats")
    public R<Map<String, Object>> projectStats() {
        return R.ok(adminService.getProjectStats());
    }

    /**
     * 更新作品状态
     */
    @Operation(summary = "更新作品状态")
    @RequireRole("ADMIN")
    @PutMapping("/project/{id}/status")
    public R<Void> updateProjectStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return R.fail(400, "状态不能为空");
        }
        adminService.updateProjectStatus(id, status);
        return R.ok();
    }

    /**
     * 删除作品
     */
    @Operation(summary = "删除作品")
    @RequireRole("ADMIN")
    @DeleteMapping("/project/{id}")
    public R<Void> deleteProject(@PathVariable Long id) {
        adminService.deleteProject(id);
        return R.ok();
    }

    // ==================== 评论管理 ====================

    /**
     * 评论列表（分页）
     */
    @Operation(summary = "评论列表")
    @RequireRole("ADMIN")
    @GetMapping("/comment")
    public R<?> listComments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(adminService.listComments(keyword, page, size));
    }

    /**
     * 删除评论
     */
    @Operation(summary = "删除评论")
    @RequireRole("ADMIN")
    @DeleteMapping("/comment/{id}")
    public R<Void> deleteComment(@PathVariable Long id) {
        adminService.deleteComment(id);
        return R.ok();
    }
}
