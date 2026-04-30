package com.scratch.community.module.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.event.EventPublisherHelper;
import com.scratch.community.common.event.PointEvent;
import com.scratch.community.common.event.ProjectCommentEvent;
import com.scratch.community.common.event.ProjectLikeEvent;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.common.audit.SensitiveWordFilter;
import com.scratch.community.common.repository.CrossModuleQueryRepository;
import com.scratch.community.common.repository.CrossModuleWriteRepository;
import com.scratch.community.module.social.dto.AddCommentDTO;
import com.scratch.community.module.social.entity.ProjectComment;
import com.scratch.community.module.social.entity.ProjectLike;
import com.scratch.community.module.social.mapper.ProjectCommentMapper;
import com.scratch.community.module.social.mapper.ProjectLikeMapper;
import com.scratch.community.module.social.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社区服务（点赞 + 评论）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialService {

    private final ProjectLikeMapper projectLikeMapper;
    private final ProjectCommentMapper projectCommentMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final CrossModuleQueryRepository crossModuleQuery;
    private final CrossModuleWriteRepository crossModuleWrite;
    private final EventPublisherHelper eventPublisher;

    // ==================== 点赞 ====================

    /**
     * 点赞项目
     * @return true=新点赞成功, false=已点赞（幂等）
     *
     * <p>并发安全: 使用 INSERT IGNORE + 事件驱动递增，避免 check-then-insert 竞态。
     * 即使两个请求同时通过检查，INSERT IGNORE 会静默忽略重复插入，
     * 只有实际插入成功的请求才会发布点赞事件和积分事件。
     *
     * <p>事件驱动: 点赞计数通过 {@link ProjectLikeEvent} 异步更新，
     * 解耦 social 模块对 project 表的直接写操作。
     */
    @Transactional
    public boolean like(Long userId, Long projectId) {
        // 检查项目是否存在
        if (!projectExists(projectId)) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }

        // 使用 INSERT IGNORE 避免竞态：唯一约束 uk_user_project 保证幂等
        int inserted = crossModuleWrite.insertIgnoreLike(userId, projectId);

        if (inserted == 0) {
            return false; // 已点赞（唯一约束冲突），幂等返回
        }

        // 发布点赞事件：editor 模块在事务提交后更新 project.like_count
        eventPublisher.publishEvent(
                new ProjectLikeEvent(this, projectId, userId, ProjectLikeEvent.LikeAction.LIKE),
                "点赞事件",
                () -> crossModuleWrite.incrementProjectLikeCount(projectId)
        );

        // 发布积分事件：项目作者获得点赞积分
        Long projectOwnerId = crossModuleQuery.getProjectOwnerId(projectId);
        if (projectOwnerId != null) {
            eventPublisher.publishEvent(
                    new PointEvent(this, projectOwnerId, PointEvent.PointAction.RECEIVE_LIKE, projectId),
                    "点赞积分事件"
            );
        }

        log.info("用户点赞项目: userId={}, projectId={}", userId, projectId);
        return true;
    }

    /**
     * 取消点赞
     * @return true=取消成功, false=未点赞（幂等）
     */
    @Transactional
    public boolean unlike(Long userId, Long projectId) {
        int deleted = projectLikeMapper.delete(
                new LambdaQueryWrapper<ProjectLike>()
                        .eq(ProjectLike::getUserId, userId)
                        .eq(ProjectLike::getProjectId, projectId));
        if (deleted > 0) {
            // 发布取消点赞事件：editor 模块在事务提交后更新 project.like_count
            eventPublisher.publishEvent(
                    new ProjectLikeEvent(this, projectId, userId, ProjectLikeEvent.LikeAction.UNLIKE),
                    "取消点赞事件",
                    () -> crossModuleWrite.decrementProjectLikeCount(projectId)
            );
            log.info("用户取消点赞: userId={}, projectId={}", userId, projectId);
            return true;
        }
        return false;
    }

    /**
     * 检查用户是否已点赞某项目
     */
    @Transactional(readOnly = true)
    public boolean isLiked(Long userId, Long projectId) {
        return projectLikeMapper.countByUserAndProject(userId, projectId) > 0;
    }

    // ==================== 评论 ====================

    /**
     * 添加评论
     */
    @Transactional
    public CommentVO addComment(Long userId, AddCommentDTO dto) {
        // 检查项目是否存在
        if (!projectExists(dto.getProjectId())) {
            throw new BizException(ErrorCode.PROJECT_NOT_FOUND);
        }

        // 敏感词过滤
        try {
            sensitiveWordFilter.check(dto.getContent());
        } catch (BizException e) {
            throw new BizException(ErrorCode.COMMENT_INVALID);
        }

        ProjectComment comment = new ProjectComment();
        comment.setUserId(userId);
        comment.setProjectId(dto.getProjectId());
        comment.setContent(dto.getContent());
        projectCommentMapper.insert(comment);

        // 发布评论事件：editor 模块在事务提交后更新 project.comment_count
        eventPublisher.publishEvent(
                new ProjectCommentEvent(this, dto.getProjectId(), userId, ProjectCommentEvent.CommentAction.ADD),
                "评论事件",
                () -> crossModuleWrite.incrementProjectCommentCount(dto.getProjectId())
        );

        // 返回评论（含用户信息）
        return getCommentById(comment.getId());
    }

    /**
     * 删除评论（仅自己或管理员）
     */
    @Transactional
    public void deleteComment(Long userId, Long commentId, String userRole) {
        ProjectComment comment = projectCommentMapper.selectById(commentId);
        if (comment == null) {
            return;
        }
        if (!comment.getUserId().equals(userId) && !"ADMIN".equals(userRole)) {
            throw new BizException(ErrorCode.USER_NO_PERMISSION);
        }
        projectCommentMapper.deleteById(commentId);

        // 发布删除评论事件：editor 模块在事务提交后更新 project.comment_count
        eventPublisher.publishEvent(
                new ProjectCommentEvent(this, comment.getProjectId(), userId, ProjectCommentEvent.CommentAction.DELETE),
                "删除评论事件",
                () -> crossModuleWrite.decrementProjectCommentCount(comment.getProjectId())
        );

        log.info("删除评论: userId={}, commentId={}", userId, commentId);
    }

    /**
     * 获取项目评论列表（分页）
     */
    @Transactional(readOnly = true)
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<CommentVO> getComments(
            Long projectId, com.baomidou.mybatisplus.extension.plugins.pagination.Page<CommentVO> page) {
        return projectCommentMapper.selectCommentsByProjectId(page, projectId);
    }

    // ==================== 私有方法 ====================

    /**
     * 检查项目是否存在（通过 CrossModuleQueryRepository 跨模块查询）
     */
    private boolean projectExists(Long projectId) {
        return crossModuleQuery.projectExists(projectId);
    }

    private CommentVO getCommentById(Long commentId) {
        ProjectComment comment = projectCommentMapper.selectById(commentId);
        if (comment == null) {
            return null;
        }
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setProjectId(comment.getProjectId());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        // 查询用户信息
        try {
            java.util.Map<String, Object> userInfo = crossModuleQuery.getUserBasicInfo(comment.getUserId());
            if (userInfo != null) {
                vo.setUsername((String) userInfo.get("username"));
                vo.setNickname((String) userInfo.get("nickname"));
                vo.setAvatarUrl((String) userInfo.get("avatar_url"));
            }
        } catch (Exception e) {
            log.warn("查询评论用户信息失败: userId={}", comment.getUserId());
        }
        return vo;
    }
}
