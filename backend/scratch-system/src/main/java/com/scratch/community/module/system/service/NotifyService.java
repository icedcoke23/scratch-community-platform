package com.scratch.community.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.system.entity.Notification;
import com.scratch.community.module.system.mapper.NotificationMapper;
import com.scratch.community.module.system.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NotificationMapper notificationMapper;

    /**
     * 创建通知
     */
    @Transactional
    public void create(Long userId, String type, String title, String content, String data) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setData(data);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        log.info("创建通知: userId={}, type={}, title={}", userId, type, title);
    }

    /**
     * 查询我的通知列表（分页）
     */
    @Transactional(readOnly = true)
    public Page<NotificationVO> myNotifications(Long userId, Page<Notification> page) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);

        Page<Notification> result = notificationMapper.selectPage(page, wrapper);
        Page<NotificationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 标记单条通知已读
     */
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (notification.getIsRead() == 0) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
    }

    /**
     * 获取未读通知数量
     */
    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {
        Long count = notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
        return count != null ? count.intValue() : 0;
    }

    /**
     * 标记全部通知已读
     */
    @Transactional
    public void markAllRead(Long userId) {
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        log.info("全部通知已读: userId={}", userId);
    }

    private NotificationVO toVO(Notification entity) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
