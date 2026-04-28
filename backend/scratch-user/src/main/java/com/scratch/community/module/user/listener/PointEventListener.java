package com.scratch.community.module.user.listener;

import com.scratch.community.common.event.PointEvent;
import com.scratch.community.module.user.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 积分事件监听器
 *
 * <p>监听各模块发布的积分事件，在事务提交后处理。
 * 使用 {@code @TransactionalEventListener(AFTER_COMMIT)} 确保积分操作
 * 在发布方事务提交后执行，避免读取未提交数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventListener {

    private final PointService pointService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPointEvent(PointEvent event) {
        try {
            switch (event.getAction()) {
                case DAILY_CHECKIN -> pointService.dailyCheckin(event.getUserId());
                case PUBLISH_PROJECT -> pointService.onProjectPublished(event.getUserId(), event.getRefId());
                case RECEIVE_LIKE -> pointService.onReceiveLike(event.getUserId(), event.getRefId());
                case AC_SUBMISSION -> pointService.onACSubmission(event.getUserId(), event.getRefId());
                case COMPLETE_HOMEWORK -> pointService.onHomeworkComplete(event.getUserId(), event.getRefId());
                default -> log.warn("未知积分动作: {}", event.getAction());
            }
        } catch (Exception e) {
            log.error("积分事件处理失败: userId={}, action={}, error={}",
                    event.getUserId(), event.getAction(), e.getMessage());
        }
    }
}
