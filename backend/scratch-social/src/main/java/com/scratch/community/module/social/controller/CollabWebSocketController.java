package com.scratch.community.module.social.controller;

import com.scratch.community.module.social.dto.CollabMessage;
import com.scratch.community.module.social.dto.EditOperation;
import com.scratch.community.module.social.service.CollabService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 协作编辑 WebSocket 控制器
 *
 * 客户端通过 STOMP 协议发送消息到:
 * - /app/collab/{sessionId}/join     — 加入会话
 * - /app/collab/{sessionId}/leave    — 离开会话
 * - /app/collab/{sessionId}/edit     — 提交编辑操作
 * - /app/collab/{sessionId}/cursor   — 更新光标位置
 * - /app/collab/{sessionId}/chat     — 发送聊天消息
 *
 * 安全说明: userId 从 STOMP Principal 中获取（握手时由 JWT 解析设置）
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CollabWebSocketController {

    private final CollabService collabService;

    /**
     * 从 STOMP 消息头中提取 userId
     * WebSocket 配置中已通过 JWT 解析设置了 Principal (name = userId)
     */
    private Long resolveUserId(Principal principal) {
        if (principal == null || "anonymous".equals(principal.getName())) {
            log.warn("WebSocket 消息缺少用户身份");
            return null;
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.warn("WebSocket Principal 格式错误: {}", principal.getName());
            return null;
        }
    }

    /**
     * 加入协作会话
     */
    @MessageMapping("/collab/{sessionId}/join")
    public void joinSession(@DestinationVariable Long sessionId,
                            @Payload CollabMessage message,
                            Principal principal) {
        Long userId = resolveUserId(principal);
        if (userId == null) return;

        String role = "viewer";
        if (message.getPayload() instanceof Map<?, ?> payload) {
            role = String.valueOf(payload.get("role") != null ? payload.get("role") : "viewer");
        }
        collabService.joinSession(sessionId, userId, role);
    }

    /**
     * 离开协作会话
     */
    @MessageMapping("/collab/{sessionId}/leave")
    public void leaveSession(@DestinationVariable Long sessionId,
                             Principal principal) {
        Long userId = resolveUserId(principal);
        if (userId == null) return;
        collabService.leaveSession(sessionId, userId);
    }

    /**
     * 提交编辑操作
     */
    @MessageMapping("/collab/{sessionId}/edit")
    public void handleEdit(@DestinationVariable Long sessionId,
                           @Payload EditOperation operation,
                           Principal principal) {
        Long userId = resolveUserId(principal);
        if (userId == null) return;
        collabService.handleEdit(sessionId, userId, operation);
    }

    /**
     * 更新光标位置
     */
    @MessageMapping("/collab/{sessionId}/cursor")
    public void updateCursor(@DestinationVariable Long sessionId,
                             @Payload Map<String, Integer> cursor,
                             Principal principal) {
        Long userId = resolveUserId(principal);
        if (userId == null) return;
        int x = cursor.getOrDefault("x", 0);
        int y = cursor.getOrDefault("y", 0);
        collabService.updateCursor(sessionId, userId, x, y);
    }

    /**
     * 发送聊天消息
     */
    @MessageMapping("/collab/{sessionId}/chat")
    public void sendChat(@DestinationVariable Long sessionId,
                         @Payload CollabMessage message,
                         Principal principal) {
        Long userId = resolveUserId(principal);
        if (userId == null) return;
        String text = "";
        if (message.getPayload() instanceof Map<?, ?> payload) {
            text = String.valueOf(payload.get("message") != null ? payload.get("message") : "");
        }
        collabService.sendChat(sessionId, userId, text);
    }
}
