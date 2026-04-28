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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // ==================== 私有方法 ====================

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
