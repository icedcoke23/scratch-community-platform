package com.scratch.community.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.auth.RequireRole;
import com.scratch.community.common.result.R;
import com.scratch.community.module.system.dto.AuditActionDTO;
import com.scratch.community.module.system.dto.UpdateConfigDTO;
import com.scratch.community.module.system.service.AuditService;
import com.scratch.community.module.system.service.ConfigService;
import com.scratch.community.module.system.service.NotifyService;
import com.scratch.community.module.system.vo.AuditLogVO;
import com.scratch.community.module.system.vo.ConfigVO;
import com.scratch.community.module.system.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统管理 Controller
 * 包含通知管理、审核管理、配置管理
 */
@Tag(name = "系统管理", description = "通知/审核/配置")
@RestController
@RequiredArgsConstructor
public class SystemController {

    private final NotifyService notifyService;
    private final AuditService auditService;
    private final ConfigService configService;

    // ==================== 通知接口 ====================

    /**
     * 我的通知列表（分页）
     */
    @Operation(summary = "我的通知列表")
    @GetMapping("/api/v1/notification")
    public R<Page<NotificationVO>> myNotifications(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Long userId = LoginUser.getUserId();
        Page<NotificationVO> result = notifyService.myNotifications(userId, new Page<>(page, size));
        return R.ok(result);
    }

    /**
     * 获取未读通知数量
     */
    @Operation(summary = "获取未读通知数量")
    @GetMapping("/api/v1/notification/unread-count")
    public R<Integer> unreadCount() {
        Long userId = LoginUser.getUserId();
        return R.ok(notifyService.getUnreadCount(userId));
    }

    /**
     * 标记单条通知已读
     */
    @Operation(summary = "标记通知已读")
    @PutMapping("/api/v1/notification/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        Long userId = LoginUser.getUserId();
        notifyService.markRead(userId, id);
        return R.ok();
    }

    /**
     * 全部通知已读
     */
    @Operation(summary = "全部通知已读")
    @PutMapping("/api/v1/notification/read-all")
    public R<Void> markAllRead() {
        Long userId = LoginUser.getUserId();
        notifyService.markAllRead(userId);
        return R.ok();
    }

    // ==================== 审核接口（管理员） ====================

    /**
     * 审核记录列表（管理员）
     */
    @RequireRole("ADMIN")
    @Operation(summary = "审核记录列表")
    @GetMapping("/api/v1/admin/audit")
    public R<Page<AuditLogVO>> listAuditLogs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Page<AuditLogVO> result = auditService.listAuditLogs(status, new Page<>(page, size));
        return R.ok(result);
    }

    /**
     * 审核操作（通过/拒绝）
     */
    @RequireRole("ADMIN")
    @Operation(summary = "审核操作")
    @PutMapping("/api/v1/admin/audit/{id}")
    public R<Void> auditAction(@PathVariable Long id, @Valid @RequestBody AuditActionDTO dto) {
        Long operatorId = LoginUser.getUserId();
        auditService.auditAction(id, operatorId, dto);
        return R.ok();
    }

    // ==================== 配置接口（管理员） ====================

    /**
     * 系统配置列表（管理员）
     */
    @RequireRole("ADMIN")
    @Operation(summary = "系统配置列表")
    @GetMapping("/api/v1/admin/config")
    public R<List<ConfigVO>> listConfigs() {
        List<ConfigVO> result = configService.listAll();
        return R.ok(result);
    }

    /**
     * 更新配置
     */
    @RequireRole("ADMIN")
    @Operation(summary = "更新配置")
    @PutMapping("/api/v1/admin/config/{key}")
    public R<Void> updateConfig(@PathVariable String key, @Valid @RequestBody UpdateConfigDTO dto) {
        configService.updateConfig(key, dto.getConfigValue(), dto.getDescription());
        return R.ok();
    }
}
