package com.scratch.community.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scratch.community.common.auth.JwtUtils;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.common.util.FileUploadUtils;
import com.scratch.community.module.user.dto.*;
import com.scratch.community.module.user.entity.User;
import com.scratch.community.module.user.entity.UserFollow;
import com.scratch.community.module.user.mapper.UserFollowMapper;
import com.scratch.community.module.user.mapper.UserMapper;
import com.scratch.community.module.user.vo.LoginVO;
import com.scratch.community.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final FileUploadUtils fileUploadUtils;
    private final JwtUtils jwtUtils;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 注册
     */
    @Transactional
    public LoginVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException(ErrorCode.USER_EXISTS);
        }

        // 检查邮箱唯一性（如果提供了邮箱）
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
            if (emailCount > 0) {
                throw new BizException(ErrorCode.USER_EXISTS.getCode(), "邮箱已被注册");
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole() != null ? dto.getRole() : "STUDENT");
        user.setStatus(1);
        userMapper.insert(user);

        // 注册后自动生成 Token（自动登录）
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusSeconds(jwtUtils.getRefreshTokenExpiry(refreshToken).getTime() / 1000 - System.currentTimeMillis() / 1000));
        userMapper.updateById(user);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUserInfo(toVO(user));
        return loginVO;
    }

    /**
     * 登录
     */
    @Transactional
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (!PASSWORD_ENCODER.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        // 存储 Refresh Token 到用户记录
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusSeconds(jwtUtils.getRefreshTokenExpiry(refreshToken).getTime() / 1000 - System.currentTimeMillis() / 1000));
        userMapper.updateById(user);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUserInfo(toVO(user));
        return loginVO;
    }

    /**
     * 获取用户信息
     */
    @Transactional(readOnly = true)
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    /**
     * 根据 ID 获取用户实体（用于 Token 刷新等内部操作）
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 更新用户的 Refresh Token
     *
     * @param userId       用户 ID
     * @param refreshToken 新的 Refresh Token
     * @param expiresAt    过期时间
     */
    @Transactional
    public void updateRefreshToken(Long userId, String refreshToken, java.util.Date expiresAt) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiresAt(expiresAt != null ?
                java.time.LocalDateTime.ofInstant(expiresAt.toInstant(), java.time.ZoneId.systemDefault()) : null);
            userMapper.updateById(user);
        }
    }

    /**
     * 清除用户的 Refresh Token（登出时调用）
     *
     * @param userId 用户 ID
     */
    @Transactional
    public void clearRefreshToken(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiresAt(null);
            userMapper.updateById(user);
        }
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public void updateUser(Long userId, UpdateUserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getBio() != null) user.setBio(dto.getBio());
        userMapper.updateById(user);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (!PASSWORD_ENCODER.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        user.setPassword(PASSWORD_ENCODER.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    /**
     * 上传头像
     */
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        String key = fileUploadUtils.uploadAvatar(file);
        String url = fileUploadUtils.getUrl("avatar", key);

        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setAvatarUrl(url);
            userMapper.updateById(user);
        }
        return url;
    }

    /**
     * 关注用户
     *
     * <p>并发安全: 使用 INSERT IGNORE 避免 check-then-insert 竞态。
     * 唯一约束 uk_follow 保证幂等，重复关注静默成功。
     */
    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "不能关注自己");
        }
        if (userMapper.selectById(followingId) == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        // INSERT IGNORE: 唯一约束 uk_follow 保证幂等，并发安全
        userFollowMapper.insertIgnore(followerId, followingId);
    }

    /**
     * 取消关注
     */
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        userFollowMapper.delete(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, followerId)
                        .eq(UserFollow::getFollowingId, followingId));
    }

    /**
     * 获取关注列表
     */
    @Transactional(readOnly = true)
    public List<UserVO> getFollowing(Long userId) {
        return userFollowMapper.selectFollowing(userId);
    }

    /**
     * 获取粉丝列表
     */
    @Transactional(readOnly = true)
    public List<UserVO> getFollowers(Long userId) {
        return userFollowMapper.selectFollowers(userId);
    }

    /**
     * 搜索用户（分页）
     */
    @Transactional(readOnly = true)
    public Page<UserVO> searchUsers(String keyword, Page<User> page) {
        Page<User> result = userMapper.selectPage(page,
                new LambdaQueryWrapper<User>()
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getNickname, keyword)
                        .orderByDesc(User::getCreatedAt));
        return toVOPage(result);
    }

    // ==================== 私有方法 ====================

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    private Page<UserVO> toVOPage(Page<User> page) {
        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }
}
