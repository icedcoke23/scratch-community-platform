package com.scratch.community.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 积分事件
 * 各模块发布此事件，user 模块的 PointService 监听并处理
 */
@Getter
public class PointEvent extends ApplicationEvent {

    private final Long userId;
    private final PointAction action;
    private final Long refId;

    public PointEvent(Object source, Long userId, PointAction action, Long refId) {
        super(source);
        this.userId = userId;
        this.action = action;
        this.refId = refId;
    }

    /**
     * 积分动作枚举
     */
    public enum PointAction {
        /** 每日签到 */
        DAILY_CHECKIN,
        /** 发布项目 */
        PUBLISH_PROJECT,
        /** 收到点赞 */
        RECEIVE_LIKE,
        /** 判题通过 */
        AC_SUBMISSION,
        /** 完成作业 */
        COMPLETE_HOMEWORK
    }
}
