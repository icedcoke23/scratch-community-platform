package com.scratch.community.module.social.dto;

import lombok.Data;

/**
 * WebSocket 消息基类
 */
@Data
public class CollabMessage {
    /** 消息类型: join / leave / edit / cursor / chat */
    private String type;
    /** 会话 ID */
    private Long sessionId;
    /** 消息载荷 */
    private Object payload;
}
