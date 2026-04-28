package com.scratch.community.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 项目点赞事件
 *
 * <p>当用户点赞/取消点赞时由 social 模块发布，
 * editor 模块监听并更新 project.like_count。
 *
 * @author scratch-community
 */
@Getter
public class ProjectLikeEvent extends ApplicationEvent {

    private final Long projectId;
    private final Long userId;
    private final LikeAction action;

    public ProjectLikeEvent(Object source, Long projectId, Long userId, LikeAction action) {
        super(source);
        this.projectId = projectId;
        this.userId = userId;
        this.action = action;
    }

    /**
     * 点赞动作枚举
     */
    public enum LikeAction {
        /** 点赞（+1） */
        LIKE,
        /** 取消点赞（-1） */
        UNLIKE
    }
}
