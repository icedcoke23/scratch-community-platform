package com.scratch.community.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 项目评论事件
 *
 * <p>当用户添加/删除评论时由 social 模块发布，
 * editor 模块监听并更新 project.comment_count。
 *
 * @author scratch-community
 */
@Getter
public class ProjectCommentEvent extends ApplicationEvent {

    private final Long projectId;
    private final Long userId;
    private final CommentAction action;

    public ProjectCommentEvent(Object source, Long projectId, Long userId, CommentAction action) {
        super(source);
        this.projectId = projectId;
        this.userId = userId;
        this.action = action;
    }

    /**
     * 评论动作枚举
     */
    public enum CommentAction {
        /** 添加评论（+1） */
        ADD,
        /** 删除评论（-1） */
        DELETE
    }
}
