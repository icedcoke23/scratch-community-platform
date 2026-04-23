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
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final JwtUtils jwtUtils;
    private final FileUploadUtils fileUploadUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 注册
     */
    @Transactional
    public UserVO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BizException(ErrorCode.USER_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRole(dto.getRole() != null ? dto.getRole() : "STUDENT");
        user.setStatus(1);
        userMapper.insert(user);

        return toVO(user);
    }

    /**
     * 登录 → JWT Token
     */
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }

        // 生成 Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO result = new LoginVO();
        result.setToken(token);
        LoginVO.UserVO userVO = new LoginVO.UserVO();
        BeanUtils.copyProperties(user, userVO);
        result.setUser(userVO);

        return result;
    }

    /**
     * 获取用户信息
     */
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    /**
     * 更新个人信息
     */
    public void updateUser(Long userId, UpdateUserDTO dto) {
        User user = new User();
        user.setId(userId);
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getBio() != null) user.setBio(dto.getBio());
        userMapper.updateById(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    /**
     * 上传头像
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        String key = fileUploadUtils.upload(file, "avatar");
        String url = fileUploadUtils.getUrl("avatar", key);

        User update = new User();
        update.setId(userId);
        update.setAvatarUrl(url);
        userMapper.updateById(update);

        return url;
    }

    /**
     * 关注
     */
    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BizException(9998, "不能关注自己");
        }

        // 检查是否已关注
        Long count = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, followerId)
                        .eq(UserFollow::getFollowingId, followingId)
        );
        if (count > 0) return; // 已关注，幂等处理

        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        userFollowMapper.insert(follow);
    }

    /**
     * 取消关注
     */
    public void unfollow(Long followerId, Long followingId) {
        userFollowMapper.delete(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, followerId)
                        .eq(UserFollow::getFollowingId, followingId)
        );
    }

    /**
     * 粉丝列表
     */
    public Page<UserVO> getFollowers(Long userId, Page<UserFollow> page) {
        // TODO: 通过 JOIN 查询粉丝信息
        return new Page<>();
    }

    /**
     * 关注列表
     */
    public Page<UserVO> getFollowing(Long userId, Page<UserFollow> page) {
        // TODO: 通过 JOIN 查询关注的人
        return new Page<>();
    }

    /**
     * 搜索用户
     */
    public Page<UserVO> searchUsers(String keyword, Page<User> page) {
        Page<User> result = userMapper.selectPage(page,
                new LambdaQueryWrapper<User>()
                        .like(User::getNickname, keyword)
                        .or()
                        .like(User::getUsername, keyword)
                        .orderByDesc(User::getCreatedAt)
        );
        // TODO: 转换为 UserVO
        return new Page<>();
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
