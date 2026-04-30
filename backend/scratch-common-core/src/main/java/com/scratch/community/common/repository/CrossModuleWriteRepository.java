package com.scratch.community.common.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 跨模块写操作仓库
 *
 * 将散落在各 Service 中的跨模块 JdbcTemplate 写操作集中管理，
 * 与 {@link CrossModuleQueryRepository}（只读）形成读写分离。
 *
 * <p>设计原则:
 * <ul>
 *   <li>所有跨模块的裸 SQL <b>写操作</b>（INSERT / UPDATE / DELETE）都应放在这里</li>
 *   <li>本类只提供<b>写操作</b>方法，只读查询请使用 {@link CrossModuleQueryRepository}</li>
 *   <li>所有操作使用参数化防止 SQL 注入</li>
 *   <li>方法命名清晰表达操作意图</li>
 * </ul>
 *
 * <p>模块边界:
 * 各模块通过此 Repository 执行跨模块的写操作，而非直接注入 JdbcTemplate。
 * 未来如果需要模块间解耦（如微服务拆分），只需替换此 Repository 的实现。
 *
 * @see CrossModuleQueryRepository 跨模块只读查询仓库
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CrossModuleWriteRepository {

    private final JdbcTemplate jdbcTemplate;

    // ==================== 社区模块写操作 ====================

    /**
     * INSERT IGNORE 点赞记录（原子操作，避免竞态）
     *
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @return 实际插入行数（0=已点赞，1=新点赞）
     */
    public int insertIgnoreLike(Long userId, Long projectId) {
        return jdbcTemplate.update(
                "INSERT IGNORE INTO project_like (user_id, project_id) VALUES (?, ?)",
                userId, projectId);
    }

    /**
     * 插入评论
     *
     * @param userId 用户 ID
     * @param projectId 项目 ID
     * @param content 评论内容
     */
    public void insertComment(Long userId, Long projectId, String content) {
        jdbcTemplate.update(
                "INSERT INTO project_comment (user_id, project_id, content) VALUES (?, ?, ?)",
                userId, projectId, content);
    }

    /**
     * 删除评论
     *
     * @param commentId 评论 ID
     * @return 实际删除行数
     */
    public int deleteComment(Long commentId) {
        return jdbcTemplate.update("DELETE FROM project_comment WHERE id = ?", commentId);
    }

    // ==================== 项目计数写操作 ====================

    /**
     * 原子递增项目浏览数
     *
     * @param projectId 项目 ID
     */
    public void incrementProjectViewCount(Long projectId) {
        jdbcTemplate.update("UPDATE project SET view_count = view_count + 1 WHERE id = ?", projectId);
    }

    /**
     * 原子递增项目 Remix 次数
     *
     * @param projectId 项目 ID
     */
    public void incrementProjectRemixCount(Long projectId) {
        jdbcTemplate.update("UPDATE project SET remix_count = remix_count + 1 WHERE id = ?", projectId);
    }

    /**
     * 原子递增项目点赞数
     *
     * @param projectId 项目 ID
     */
    public void incrementProjectLikeCount(Long projectId) {
        jdbcTemplate.update("UPDATE project SET like_count = like_count + 1 WHERE id = ?", projectId);
    }

    /**
     * 原子递减项目点赞数（不低于 0）
     *
     * @param projectId 项目 ID
     */
    public void decrementProjectLikeCount(Long projectId) {
        jdbcTemplate.update("UPDATE project SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?", projectId);
    }

    /**
     * 原子递增项目评论数
     *
     * @param projectId 项目 ID
     */
    public void incrementProjectCommentCount(Long projectId) {
        jdbcTemplate.update("UPDATE project SET comment_count = comment_count + 1 WHERE id = ?", projectId);
    }

    /**
     * 原子递减项目评论数（不低于 0）
     *
     * @param projectId 项目 ID
     */
    public void decrementProjectCommentCount(Long projectId) {
        jdbcTemplate.update("UPDATE project SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = ?", projectId);
    }

    // ==================== 用户写操作（积分模块需要） ====================

    /**
     * 原子更新用户积分和等级
     *
     * @param userId 用户 ID
     * @param newPoints 新积分值
     * @param newLevel 新等级值
     */
    public void updateUserPointsAndLevel(Long userId, int newPoints, int newLevel) {
        jdbcTemplate.update("UPDATE user SET points = ?, level = ? WHERE id = ?", newPoints, newLevel, userId);
    }

    /**
     * 原子更新用户等级（仅当等级变化时更新）
     *
     * @param userId 用户 ID
     * @param newLevel 新等级值
     */
    public void updateUserLevel(Long userId, int newLevel) {
        jdbcTemplate.update("UPDATE user SET level = ? WHERE id = ? AND level != ?", newLevel, userId, newLevel);
    }

    /**
     * 插入积分变动记录（含 total_points 快照）
     *
     * @param userId 用户 ID
     * @param type 积分类型
     * @param points 积分变动值
     * @param totalPoints 变动后总积分快照
     * @param refType 关联类型
     * @param refId 关联 ID
     * @param remark 备注
     */
    public void insertPointLog(Long userId, String type, int points, int totalPoints, String refType, Long refId, String remark) {
        jdbcTemplate.update(
                "INSERT INTO point_log (user_id, type, points, total_points, ref_type, ref_id, remark) VALUES (?, ?, ?, ?, ?, ?, ?)",
                userId, type, points, totalPoints, refType, refId, remark);
    }
}
