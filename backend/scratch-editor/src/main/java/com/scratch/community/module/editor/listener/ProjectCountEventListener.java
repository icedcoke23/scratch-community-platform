package com.scratch.community.module.editor.listener;

import com.scratch.community.common.event.ProjectCommentEvent;
import com.scratch.community.common.event.ProjectLikeEvent;
import com.scratch.community.common.event.ProjectViewEvent;
import com.scratch.community.common.repository.CrossModuleWriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 项目计数事件监听器
 *
 * <p>监听 social 模块发布的点赞/评论/浏览事件，
 * 在事务提交后更新 project 表中的冗余计数字段。
 *
 * <p>设计原则：
 * <ul>
 *   <li>使用 {@code @TransactionalEventListener(AFTER_COMMIT)} 确保在发布方事务提交后执行</li>
 *   <li>通过 {@link CrossModuleWriteRepository} 执行原子递增/递减</li>
 *   <li>事件处理失败不影响主流程（发布方已提交事务）</li>
 * </ul>
 *
 * @author scratch-community
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectCountEventListener {

    private final CrossModuleWriteRepository crossModuleWrite;

    /**
     * 处理项目点赞事件（事务提交后执行）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectLike(ProjectLikeEvent event) {
        try {
            if (event.getAction() == ProjectLikeEvent.LikeAction.LIKE) {
                crossModuleWrite.incrementProjectLikeCount(event.getProjectId());
                log.debug("点赞计数+1: projectId={}", event.getProjectId());
            } else {
                crossModuleWrite.decrementProjectLikeCount(event.getProjectId());
                log.debug("点赞计数-1: projectId={}", event.getProjectId());
            }
        } catch (Exception e) {
            log.error("处理点赞事件失败: projectId={}, action={}, error={}",
                    event.getProjectId(), event.getAction(), e.getMessage());
        }
    }

    /**
     * 处理项目评论事件（事务提交后执行）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectComment(ProjectCommentEvent event) {
        try {
            if (event.getAction() == ProjectCommentEvent.CommentAction.ADD) {
                crossModuleWrite.incrementProjectCommentCount(event.getProjectId());
                log.debug("评论计数+1: projectId={}", event.getProjectId());
            } else {
                crossModuleWrite.decrementProjectCommentCount(event.getProjectId());
                log.debug("评论计数-1: projectId={}", event.getProjectId());
            }
        } catch (Exception e) {
            log.error("处理评论事件失败: projectId={}, action={}, error={}",
                    event.getProjectId(), event.getAction(), e.getMessage());
        }
    }

    /**
     * 处理项目浏览事件（事务提交后执行）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectView(ProjectViewEvent event) {
        try {
            crossModuleWrite.incrementProjectViewCount(event.getProjectId());
            log.debug("浏览计数+1: projectId={}", event.getProjectId());
        } catch (Exception e) {
            log.error("处理浏览事件失败: projectId={}, error={}",
                    event.getProjectId(), e.getMessage());
        }
    }
}
