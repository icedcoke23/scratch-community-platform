package com.scratch.community.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 项目浏览事件
 *
 * <p>当用户浏览项目详情时由各模块发布，
 * editor 模块监听并更新 project.view_count。
 *
 * @author scratch-community
 */
@Getter
public class ProjectViewEvent extends ApplicationEvent {

    private final Long projectId;
    private final Long userId;

    public ProjectViewEvent(Object source, Long projectId, Long userId) {
        super(source);
        this.projectId = projectId;
        this.userId = userId;
    }
}
