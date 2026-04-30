package com.scratch.community.module.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.audit.SensitiveWordFilter;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.common.repository.CrossModuleWriteRepository;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.module.social.dto.AddCommentDTO;
import com.scratch.community.module.social.entity.ProjectComment;
import com.scratch.community.module.social.entity.ProjectLike;
import com.scratch.community.module.social.mapper.ProjectCommentMapper;
import com.scratch.community.module.social.mapper.ProjectLikeMapper;
import com.scratch.community.module.social.vo.CommentVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.scratch.community.common.event.EventPublisherHelper;
import com.scratch.community.common.event.ProjectLikeEvent;
import com.scratch.community.common.event.ProjectCommentEvent;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @InjectMocks
    private SocialService socialService;

    @Mock private ProjectLikeMapper projectLikeMapper;
    @Mock private ProjectCommentMapper projectCommentMapper;
    @Mock private SensitiveWordFilter sensitiveWordFilter;
    @Mock private CrossModuleQueryRepository crossModuleQuery;
    @Mock private CrossModuleWriteRepository crossModuleWrite;
    @Mock private EventPublisherHelper eventPublisher;

    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;

    @Nested
    @DisplayName("点赞测试")
    class LikeTests {

        @Test
        @DisplayName("正常点赞 -> 返回 true")
        void like_success() {
            when(crossModuleQuery.projectExists(PROJECT_ID)).thenReturn(true);
            when(crossModuleWrite.insertIgnoreLike(USER_ID, PROJECT_ID)).thenReturn(1);
            when(crossModuleQuery.getProjectOwnerId(PROJECT_ID)).thenReturn(2L);

            boolean result = socialService.like(USER_ID, PROJECT_ID);

            assertTrue(result);
            verify(eventPublisher).publishEvent(
                argThat(e -> e instanceof ProjectLikeEvent
                    && ((ProjectLikeEvent) e).getAction() == ProjectLikeEvent.LikeAction.LIKE),
                eq("点赞事件"),
                any()
            );
        }

        @Test
        @DisplayName("项目不存在 -> 抛异常")
        void like_projectNotFound() {
            when(crossModuleQuery.projectExists(PROJECT_ID)).thenReturn(false);

            assertThrows(BizException.class, () -> socialService.like(USER_ID, PROJECT_ID));
            verify(crossModuleWrite, never()).insertIgnoreLike(anyLong(), anyLong());
        }

        @Test
        @DisplayName("已点赞 -> 幂等返回 false")
        void like_alreadyLiked() {
            when(crossModuleQuery.projectExists(PROJECT_ID)).thenReturn(true);
            when(crossModuleWrite.insertIgnoreLike(USER_ID, PROJECT_ID)).thenReturn(0);

            boolean result = socialService.like(USER_ID, PROJECT_ID);

            assertFalse(result);
            verify(crossModuleWrite, never()).incrementProjectLikeCount(anyLong());
        }
    }

    @Nested
    @DisplayName("取消点赞测试")
    class UnlikeTests {

        @Test
        @DisplayName("正常取消点赞 -> 返回 true")
        void unlike_success() {
            when(projectLikeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            boolean result = socialService.unlike(USER_ID, PROJECT_ID);

            assertTrue(result);
            verify(eventPublisher).publishEvent(
                argThat(e -> e instanceof ProjectLikeEvent
                    && ((ProjectLikeEvent) e).getAction() == ProjectLikeEvent.LikeAction.UNLIKE),
                eq("取消点赞事件"),
                any()
            );
        }

        @Test
        @DisplayName("未点赞 -> 幂等返回 false")
        void unlike_notLiked() {
            when(projectLikeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);

            boolean result = socialService.unlike(USER_ID, PROJECT_ID);

            assertFalse(result);
            verify(crossModuleWrite, never()).decrementProjectLikeCount(anyLong());
        }
    }

    @Nested
    @DisplayName("评论测试")
    class CommentTests {

        @Test
        @DisplayName("正常添加评论")
        void addComment_success() {
            when(crossModuleQuery.projectExists(PROJECT_ID)).thenReturn(true);
            // 模拟 insert 时设置 ID（MyBatis-Plus 行为）
            doAnswer(invocation -> {
                ProjectComment comment = invocation.getArgument(0);
                comment.setId(100L);
                return 1;
            }).when(projectCommentMapper).insert(any(ProjectComment.class));

            ProjectComment savedComment = new ProjectComment();
            savedComment.setId(100L);
            savedComment.setUserId(USER_ID);
            savedComment.setProjectId(PROJECT_ID);
            savedComment.setContent("好棒的项目！");
            when(projectCommentMapper.selectById(100L)).thenReturn(savedComment);
            when(crossModuleQuery.getUserBasicInfo(USER_ID)).thenReturn(Map.of(
                    "username", "testuser",
                    "nickname", "测试用户",
                    "avatar_url", "https://example.com/avatar.png"
            ));

            AddCommentDTO dto = new AddCommentDTO();
            dto.setProjectId(PROJECT_ID);
            dto.setContent("好棒的项目！");

            CommentVO result = socialService.addComment(USER_ID, dto);

            assertNotNull(result);
            verify(projectCommentMapper).insert(any(ProjectComment.class));
            verify(eventPublisher).publishEvent(
                argThat(e -> e instanceof ProjectCommentEvent
                    && ((ProjectCommentEvent) e).getAction() == ProjectCommentEvent.CommentAction.ADD),
                eq("评论事件"),
                any()
            );
        }

        @Test
        @DisplayName("评论含敏感词 -> 抛异常")
        void addComment_sensitiveWord() {
            when(crossModuleQuery.projectExists(PROJECT_ID)).thenReturn(true);
            doThrow(new BizException(60001, "内容包含违规信息"))
                    .when(sensitiveWordFilter).check("这是赌博广告");

            AddCommentDTO dto = new AddCommentDTO();
            dto.setProjectId(PROJECT_ID);
            dto.setContent("这是赌博广告");

            assertThrows(BizException.class, () -> socialService.addComment(USER_ID, dto));
            verify(projectCommentMapper, never()).insert(any());
        }

        @Test
        @DisplayName("评论项目不存在 -> 抛异常")
        void addComment_projectNotFound() {
            when(crossModuleQuery.projectExists(PROJECT_ID)).thenReturn(false);

            AddCommentDTO dto = new AddCommentDTO();
            dto.setProjectId(PROJECT_ID);
            dto.setContent("评论内容");

            assertThrows(BizException.class, () -> socialService.addComment(USER_ID, dto));
        }

        @Test
        @DisplayName("删除自己的评论 -> 成功")
        void deleteComment_ownComment() {
            ProjectComment comment = new ProjectComment();
            comment.setId(100L);
            comment.setUserId(USER_ID);
            comment.setProjectId(PROJECT_ID);
            when(projectCommentMapper.selectById(100L)).thenReturn(comment);
            when(projectCommentMapper.deleteById(100L)).thenReturn(1);

            assertDoesNotThrow(() -> socialService.deleteComment(USER_ID, 100L, "STUDENT"));
            verify(eventPublisher).publishEvent(
                argThat(e -> e instanceof ProjectCommentEvent
                    && ((ProjectCommentEvent) e).getAction() == ProjectCommentEvent.CommentAction.DELETE),
                eq("删除评论事件"),
                any()
            );
        }

        @Test
        @DisplayName("删除他人评论（非管理员）-> 抛异常")
        void deleteComment_otherUser_noPermission() {
            ProjectComment comment = new ProjectComment();
            comment.setId(100L);
            comment.setUserId(999L);
            comment.setProjectId(PROJECT_ID);
            when(projectCommentMapper.selectById(100L)).thenReturn(comment);

            assertThrows(BizException.class,
                    () -> socialService.deleteComment(USER_ID, 100L, "STUDENT"));
        }

        @Test
        @DisplayName("管理员删除他人评论 -> 成功")
        void deleteComment_adminCanDelete() {
            ProjectComment comment = new ProjectComment();
            comment.setId(100L);
            comment.setUserId(999L);
            comment.setProjectId(PROJECT_ID);
            when(projectCommentMapper.selectById(100L)).thenReturn(comment);
            when(projectCommentMapper.deleteById(100L)).thenReturn(1);

            assertDoesNotThrow(() -> socialService.deleteComment(USER_ID, 100L, "ADMIN"));
        }
    }
}
