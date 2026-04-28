package com.scratch.community.module.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 服务端推送消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollabEvent {
    /** 事件类型: user_joined / user_left / edit_applied / cursor_update / conflict / session_state / error */
    private String type;
    /** 会话 ID */
    private Long sessionId;
    /** 触发事件的用户 ID */
    private Long userId;
    /** 用户昵称 */
    private String nickname;
    /** 事件载荷 */
    private Object payload;
    /** 服务器时间戳 */
    private Long timestamp;

    public static CollabEvent of(String type, Long sessionId, Long userId, String nickname, Object payload) {
        return new CollabEvent(type, sessionId, userId, nickname, payload, System.currentTimeMillis());
    }
}
