package com.scratch.community.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 事件发布辅助工具
 *
 * <p>统一封装事件发布的 try-catch + 降级逻辑，避免每个发布点都写重复的异常处理。
 *
 * <p>使用方式：
 * <pre>
 * eventPublisherHelper.publishEvent(
 *     new ProjectLikeEvent(this, projectId, userId, action),
 *     "点赞事件",
 *     () -> crossModuleWrite.incrementProjectLikeCount(projectId)  // 降级操作
 * );
 * </pre>
 *
 * @author scratch-community
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisherHelper {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发布事件，失败时执行降级操作
     *
     * @param event       事件对象
     * @param eventDesc   事件描述（用于日志）
     * @param fallback    降级操作（事件发布失败时执行）
     */
    public void publishEvent(ApplicationEvent event, String eventDesc, Runnable fallback) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("发布{}失败，降级为直接操作: error={}", eventDesc, e.getMessage());
            if (fallback != null) {
                try {
                    fallback.run();
                } catch (Exception fallbackEx) {
                    log.error("降级操作也失败: eventDesc={}, error={}", eventDesc, fallbackEx.getMessage());
                }
            }
        }
    }

    /**
     * 发布事件，失败时仅记录日志（无降级操作）
     *
     * @param event       事件对象
     * @param eventDesc   事件描述（用于日志）
     */
    public void publishEvent(ApplicationEvent event, String eventDesc) {
        publishEvent(event, eventDesc, null);
    }
}
