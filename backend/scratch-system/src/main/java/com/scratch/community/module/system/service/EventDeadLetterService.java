package com.scratch.community.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scratch.community.module.system.entity.EventDeadLetter;
import com.scratch.community.module.system.mapper.EventDeadLetterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件死信队列服务
 * 
 * 功能：
 * 1. 记录失败的事件到死信队列
 * 2. 定时重试失败的事件
 * 3. 支持人工干预和解决
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventDeadLetterService {

    private final EventDeadLetterMapper deadLetterMapper;
    private final ObjectMapper objectMapper;

    /**
     * 记录失败事件到死信队列
     */
    @Transactional
    public void recordFailure(String eventType, Object eventData, String failureReason) {
        try {
            EventDeadLetter deadLetter = new EventDeadLetter();
            deadLetter.setEventType(eventType);
            deadLetter.setEventData(objectMapper.writeValueAsString(eventData));
            deadLetter.setFailureReason(failureReason);
            deadLetter.setRetryCount(0);
            deadLetter.setMaxRetries(3);
            deadLetter.setStatus("PENDING");
            deadLetter.setNextRetryAt(LocalDateTime.now().plusMinutes(1)); // 1 分钟后首次重试
            deadLetterMapper.insert(deadLetter);
            
            log.warn("事件记录到死信队列：eventType={}, reason={}", eventType, failureReason);
        } catch (Exception e) {
            log.error("记录死信队列失败：eventType={}, error={}", eventType, e.getMessage());
        }
    }

    /**
     * 定时重试失败的事件
     * 每分钟执行一次，重试到达重试时间的事件
     */
    @Scheduled(fixedRate = 60000) // 每分钟
    @Transactional
    public void retryPendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        
        // 查询待重试的事件（状态为 PENDING 或 RETRYING，且到达重试时间）
        LambdaQueryWrapper<EventDeadLetter> wrapper = new LambdaQueryWrapper<EventDeadLetter>()
                .in(EventDeadLetter::getStatus, "PENDING", "RETRYING")
                .le(EventDeadLetter::getNextRetryAt, now)
                .lt(EventDeadLetter::getRetryCount, 3) // 最多重试 3 次
                .orderByAsc(EventDeadLetter::getNextRetryAt)
                .last("LIMIT 50"); // 每次最多处理 50 条
        
        List<EventDeadLetter> pendingEvents = deadLetterMapper.selectList(wrapper);
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        log.info("开始重试死信队列事件，数量：{}", pendingEvents.size());
        
        for (EventDeadLetter event : pendingEvents) {
            retryEvent(event);
        }
    }

    /**
     * 重试单个事件
     */
    @Transactional
    public void retryEvent(EventDeadLetter deadLetter) {
        try {
            // 更新状态为重试中
            deadLetter.setStatus("RETRYING");
            deadLetter.setRetryCount(deadLetter.getRetryCount() + 1);
            deadLetterMapper.updateById(deadLetter);
            
            // 解析事件数据
            Object eventData = objectMapper.readValue(deadLetter.getEventData(), Object.class);
            
            // 根据事件类型重新发布
            boolean success = reprocessEvent(deadLetter.getEventType(), eventData);
            
            if (success) {
                // 重试成功
                deadLetter.setStatus("SUCCESS");
                deadLetter.setResolvedAt(LocalDateTime.now());
                deadLetter.setResolutionNote("自动重试成功");
                deadLetterMapper.updateById(deadLetter);
                log.info("死信队列事件重试成功：id={}, type={}, retryCount={}", 
                        deadLetter.getId(), deadLetter.getEventType(), deadLetter.getRetryCount());
            } else {
                // 重试失败，计算下次重试时间（指数退避）
                long delayMinutes = (long) Math.pow(2, deadLetter.getRetryCount()); // 1, 2, 4, 8...
                deadLetter.setStatus("PENDING");
                deadLetter.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                deadLetterMapper.updateById(deadLetter);
                log.warn("死信队列事件重试失败，等待下次重试：id={}, type={}, retryCount={}", 
                        deadLetter.getId(), deadLetter.getEventType(), deadLetter.getRetryCount());
            }
        } catch (Exception e) {
            // 处理异常
            deadLetter.setFailureReason(e.getMessage());
            if (deadLetter.getRetryCount() >= deadLetter.getMaxRetries()) {
                deadLetter.setStatus("FAILED");
                deadLetter.setResolvedAt(LocalDateTime.now());
                log.error("死信队列事件最终失败：id={}, type={}, error={}", 
                        deadLetter.getId(), deadLetter.getEventType(), e.getMessage());
            } else {
                deadLetter.setStatus("PENDING");
                long delayMinutes = (long) Math.pow(2, deadLetter.getRetryCount());
                deadLetter.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
            }
            deadLetterMapper.updateById(deadLetter);
        }
    }

    /**
     * 重新处理事件（根据类型调用不同的处理器）
     * TODO: 需要根据实际业务实现具体的事件重放逻辑
     */
    private boolean reprocessEvent(String eventType, Object eventData) {
        // 这里需要根据事件类型调用相应的业务逻辑
        // 由于事件重放需要依赖原始的业务上下文，通常建议：
        // 1. 在原始事件处理器中实现幂等性
        // 2. 手动触发相关业务方法
        // 3. 或者记录日志后由人工干预
        
        log.debug("重放事件：type={}, data={}", eventType, eventData);
        
        // 示例：如果是积分事件，可以重新调用 PointService
        // if ("PointEvent".equals(eventType)) { ... }
        
        // 当前实现返回 false，表示需要人工干预
        // 实际项目中应根据业务需求实现具体的重放逻辑
        return false;
    }

    /**
     * 人工解决死信事件
     */
    @Transactional
    public void resolveManually(Long id, String resolvedBy, String resolutionNote) {
        EventDeadLetter deadLetter = deadLetterMapper.selectById(id);
        if (deadLetter == null) {
            throw new IllegalArgumentException("死信事件不存在：id=" + id);
        }
        
        deadLetter.setStatus("SUCCESS");
        deadLetter.setResolvedBy(resolvedBy);
        deadLetter.setResolvedAt(LocalDateTime.now());
        deadLetter.setResolutionNote(resolutionNote);
        deadLetterMapper.updateById(deadLetter);
        
        log.info("死信事件人工解决：id={}, resolvedBy={}, note={}", id, resolvedBy, resolutionNote);
    }

    /**
     * 获取死信队列统计信息
     */
    public DeadLetterStats getStats() {
        DeadLetterStats stats = new DeadLetterStats();
        
        stats.setPending(deadLetterMapper.selectCount(
                new LambdaQueryWrapper<EventDeadLetter>().eq(EventDeadLetter::getStatus, "PENDING")));
        stats.setRetrying(deadLetterMapper.selectCount(
                new LambdaQueryWrapper<EventDeadLetter>().eq(EventDeadLetter::getStatus, "RETRYING")));
        stats.setSuccess(deadLetterMapper.selectCount(
                new LambdaQueryWrapper<EventDeadLetter>().eq(EventDeadLetter::getStatus, "SUCCESS")));
        stats.setFailed(deadLetterMapper.selectCount(
                new LambdaQueryWrapper<EventDeadLetter>().eq(EventDeadLetter::getStatus, "FAILED")));
        
        return stats;
    }

    /**
     * 死信队列统计信息
     */
    @lombok.Data
    public static class DeadLetterStats {
        private Long pending;
        private Long retrying;
        private Long success;
        private Long failed;
    }
}
