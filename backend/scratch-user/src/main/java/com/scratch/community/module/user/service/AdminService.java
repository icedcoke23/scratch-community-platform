package com.scratch.community.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.user.dto.AdminUpdateUserDTO;
import com.scratch.community.module.user.entity.User;
import com.scratch.community.module.user.mapper.UserMapper;
import com.scratch.community.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 管理员服务 — 用户管理
 *
 * <p>数据统计面板已拆分到 {@link AdminDashboardService}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    // ==================== 用户管理 ====================

    /**
     * 用户列表（分页，支持搜索和角色筛选）
     */
    @Transactional(readOnly = true)
    public Page<UserVO> listUsers(String keyword, String role, Page<User> page) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword));
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(page, wrapper);
        Page<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 更新用户（角色/状态）
     */
    @Transactional
    public void updateUser(Long userId, AdminUpdateUserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getStatus() != null) {
            user.setStatus(Integer.parseInt(dto.getStatus()));
        }
        userMapper.updateById(user);
        log.info("管理员更新用户: userId={}, role={}, status={}", userId, dto.getRole(), dto.getStatus());
    }

    /**
     * 禁用用户（禁止禁用自己）
     */
    @Transactional
    public void disableUser(Long currentAdminId, Long userId) {
        if (currentAdminId.equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "不能禁用自己");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(0);
        userMapper.updateById(user);
        log.info("管理员禁用用户: userId={}", userId);
    }

    /**
     * 启用用户
     */
    @Transactional
    public void enableUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(1);
        userMapper.updateById(user);
        log.info("管理员启用用户: userId={}", userId);
    }

    // ==================== 作品管理 ====================

    /**
     * 作品列表（分页，支持搜索和状态筛选）
     * 跨模块查询，使用 JdbcTemplate
     */
    @Transactional(readOnly = true)
    public Map<String, Object> listProjects(String keyword, String status, int page, int size) {
        int offset = (page - 1) * size;

        // 构建查询条件
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE p.deleted = 0");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (p.title LIKE ? OR u.nickname LIKE ? OR u.username LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND p.status = ?");
            params.add(status);
        }

        // 总数
        String countSql = "SELECT COUNT(*) FROM project p LEFT JOIN user u ON p.user_id = u.id " + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        // 分页数据
        String dataSql = "SELECT p.id, p.user_id, u.username, u.nickname, p.title, p.description, " +
                "p.cover_url, p.status, p.block_count, p.complexity_score, " +
                "p.like_count, p.comment_count, p.view_count, p.tags, " +
                "p.remix_project_id, p.remix_count, p.created_at " +
                "FROM project p LEFT JOIN user u ON p.user_id = u.id " +
                where + " ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(size);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql, dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total != null ? total : 0);
        result.put("records", records);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 更新作品状态（发布/下线/审核）
     */
    @Transactional
    public void updateProjectStatus(Long projectId, String status) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE id = ? AND deleted = 0", Integer.class, projectId);
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "项目不存在");
        }
        jdbcTemplate.update("UPDATE project SET status = ? WHERE id = ? AND deleted = 0", status, projectId);
        log.info("管理员更新作品状态: projectId={}, status={}", projectId, status);
    }

    /**
     * 删除作品（逻辑删除）
     */
    @Transactional
    public void deleteProject(Long projectId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project WHERE id = ? AND deleted = 0", Integer.class, projectId);
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "项目不存在");
        }
        jdbcTemplate.update("UPDATE project SET deleted = 1 WHERE id = ?", projectId);
        log.info("管理员删除作品: projectId={}", projectId);
    }

    /**
     * 作品统计
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProjectStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT " +
                "(SELECT COUNT(*) FROM project WHERE deleted = 0) AS total, " +
                "(SELECT COUNT(*) FROM project WHERE deleted = 0 AND status = 'published') AS published, " +
                "(SELECT COUNT(*) FROM project WHERE deleted = 0 AND status = 'draft') AS drafts, " +
                "(SELECT COUNT(*) FROM project WHERE deleted = 0 AND status = 'reviewing') AS reviewing",
                rs -> {
                    stats.put("total", rs.getLong("total"));
                    stats.put("published", rs.getLong("published"));
                    stats.put("drafts", rs.getLong("drafts"));
                    stats.put("reviewing", rs.getLong("reviewing"));
                });
        return stats;
    }

    // ==================== 评论管理 ====================

    /**
     * 评论列表（分页，支持搜索）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> listComments(String keyword, int page, int size) {
        int offset = (page - 1) * size;

        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder("WHERE c.deleted = 0");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND c.content LIKE ?");
            params.add("%" + keyword + "%");
        }

        String countSql = "SELECT COUNT(*) FROM project_comment c " + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        String dataSql = "SELECT c.id, c.user_id, u.username, u.nickname, c.project_id, " +
                "p.title AS project_title, c.content, c.created_at " +
                "FROM project_comment c " +
                "LEFT JOIN user u ON c.user_id = u.id " +
                "LEFT JOIN project p ON c.project_id = p.id " +
                where + " ORDER BY c.created_at DESC LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(size);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql, dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total != null ? total : 0);
        result.put("records", records);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 删除评论（管理员）
     */
    @Transactional
    public void deleteComment(Long commentId) {
        // 先获取评论对应的项目ID
        Long projectId = jdbcTemplate.queryForObject(
                "SELECT project_id FROM project_comment WHERE id = ? AND deleted = 0",
                Long.class, commentId);
        if (projectId == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "评论不存在");
        }
        jdbcTemplate.update("UPDATE project_comment SET deleted = 1 WHERE id = ?", commentId);
        // 同步减少项目评论数
        jdbcTemplate.update(
                "UPDATE project SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = ?", projectId);
        log.info("管理员删除评论: commentId={}", commentId);
    }

    // ==================== 私有方法 ====================

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
