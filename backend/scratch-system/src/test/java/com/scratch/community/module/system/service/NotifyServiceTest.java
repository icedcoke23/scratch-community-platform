package com.scratch.community.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.module.system.entity.Notification;
import com.scratch.community.module.system.mapper.NotificationMapper;
import com.scratch.community.module.system.vo.NotificationVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotifyService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class NotifyServiceTest {

    @InjectMocks
    private NotifyService notifyService;

    @Mock
    private NotificationMapper notificationMapper;

    @Nested
    @DisplayName("创建通知")
    class CreateTests {

        @Test
        @DisplayName("正常创建通知")
        void create_success() {
            when(notificationMapper.insert(any(Notification.class))).thenReturn(1);

            assertDoesNotThrow(() ->
                    notifyService.create(1L, "LIKE", "收到点赞", "有人点赞了你的项目", null));

            verify(notificationMapper).insert(argThat(n ->
                    n.getUserId().equals(1L) &&
                    "LIKE".equals(n.getType()) &&
                    n.getIsRead() == 0));
        }
    }

    @Nested
    @DisplayName("查询通知")
    class QueryTests {

        @Test
        @DisplayName("分页查询通知")
        void myNotifications() {
            Notification n = new Notification();
            n.setId(1L);
            n.setUserId(1L);
            n.setType("LIKE");
            n.setTitle("收到点赞");
            n.setIsRead(0);
            n.setCreatedAt(LocalDateTime.now());

            Page<Notification> page = new Page<>(1, 20);
            page.setRecords(List.of(n));
            page.setTotal(1);

            when(notificationMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);

            Page<NotificationVO> result = notifyService.myNotifications(1L, new Page<>(1, 20));

            assertEquals(1, result.getRecords().size());
            assertEquals("收到点赞", result.getRecords().get(0).getTitle());
        }
    }

    @Nested
    @DisplayName("标记已读")
    class MarkReadTests {

        @Test
        @DisplayName("标记单条已读")
        void markRead_success() {
            Notification n = new Notification();
            n.setId(1L);
            n.setUserId(1L);
            n.setIsRead(0);
            when(notificationMapper.selectById(1L)).thenReturn(n);
            when(notificationMapper.updateById(any())).thenReturn(1);

            assertDoesNotThrow(() -> notifyService.markRead(1L, 1L));

            verify(notificationMapper).updateById(argThat(updated ->
                    updated.getIsRead() == 1));
        }

        @Test
        @DisplayName("标记他人通知抛异常")
        void markRead_forbidden() {
            Notification n = new Notification();
            n.setId(1L);
            n.setUserId(2L); // 属于其他用户
            when(notificationMapper.selectById(1L)).thenReturn(n);

            assertThrows(BizException.class,
                    () -> notifyService.markRead(1L, 1L));
        }

        @Test
        @DisplayName("已读通知不重复更新")
        void markRead_alreadyRead() {
            Notification n = new Notification();
            n.setId(1L);
            n.setUserId(1L);
            n.setIsRead(1); // 已读
            when(notificationMapper.selectById(1L)).thenReturn(n);

            notifyService.markRead(1L, 1L);

            verify(notificationMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("未读计数")
    class UnreadCountTests {

        @Test
        @DisplayName("返回未读数")
        void getUnreadCount() {
            when(notificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            int count = notifyService.getUnreadCount(1L);

            assertEquals(5, count);
        }

        @Test
        @DisplayName("无通知返回 0")
        void getUnreadCount_zero() {
            when(notificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            assertEquals(0, notifyService.getUnreadCount(1L));
        }
    }
}
