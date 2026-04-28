package com.scratch.community.module.social.controller;

import com.scratch.community.common.auth.LoginUser;
import com.scratch.community.common.result.R;
import com.scratch.community.module.social.entity.CollabSession;
import com.scratch.community.module.social.service.CollabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 协作编辑 REST API
 */
@Tag(name = "协作编辑", description = "多人实时协作编辑 Scratch 项目")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CollabController {

    private final CollabService collabService;

    @Operation(summary = "创建协作会话", description = "为指定项目创建一个协作会话，项目所有者自动成为编辑者")
    @PostMapping("/collab/session")
    public R<CollabSession> createSession(@RequestParam Long projectId) {
        Long userId = LoginUser.getUserId();
        CollabSession session = collabService.createSession(projectId, userId);
        // 创建者自动加入为 editor
        collabService.joinSession(session.getId(), userId, "editor");
        return R.ok(session);
    }

    @Operation(summary = "获取会话状态", description = "获取协作会话的当前状态，包括参与者列表和版本号")
    @GetMapping("/collab/session/{sessionId}")
    public R<Map<String, Object>> getSessionState(@PathVariable Long sessionId) {
        return R.ok(collabService.getSessionState(sessionId));
    }

    @Operation(summary = "获取项目活跃会话", description = "获取指定项目的活跃协作会话（如有）")
    @GetMapping("/collab/project/{projectId}")
    public R<CollabSession> getActiveSession(@PathVariable Long projectId) {
        CollabSession session = collabService.getActiveSession(projectId);
        return R.ok(session);
    }

    @Operation(summary = "关闭协作会话", description = "关闭协作会话，只有会话创建者可以操作")
    @PostMapping("/collab/session/{sessionId}/close")
    public R<Void> closeSession(@PathVariable Long sessionId) {
        collabService.closeSession(sessionId);
        return R.ok();
    }
}
