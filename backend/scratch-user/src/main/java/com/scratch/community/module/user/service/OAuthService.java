package com.scratch.community.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.auth.JwtUtils;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.module.user.dto.OAuthLoginDTO;
import com.scratch.community.module.user.entity.User;
import com.scratch.community.module.user.entity.UserOAuth;
import com.scratch.community.module.user.mapper.UserMapper;
import com.scratch.community.module.user.mapper.UserOAuthMapper;
import com.scratch.community.module.user.oauth.OAuthProvider;
import com.scratch.community.module.user.oauth.OAuthUserInfo;
import com.scratch.community.module.user.vo.OAuthCallbackVO;
import com.scratch.community.module.user.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第三方登录服务
 *
 * 支持微信、QQ 等第三方平台登录
 * 流程：授权码 → 获取第三方用户信息 → 绑定/注册 → 返回 JWT Token
 */
@Slf4j
@Service
public class OAuthService {

    private final UserMapper userMapper;
    private final UserOAuthMapper userOAuthMapper;
    private final JwtUtils jwtUtils;

    /** 提供者注册表 */
    private final Map<String, OAuthProvider> providers = new ConcurrentHashMap<>();

    public OAuthService(UserMapper userMapper,
                        UserOAuthMapper userOAuthMapper,
                        JwtUtils jwtUtils,
                        List<OAuthProvider> providerList) {
        this.userMapper = userMapper;
        this.userOAuthMapper = userOAuthMapper;
        this.jwtUtils = jwtUtils;

        // 自动注册所有 OAuthProvider 实现
        for (OAuthProvider provider : providerList) {
            providers.put(provider.getProviderName(), provider);
            log.info("注册 OAuth 提供者: {}", provider.getProviderName());
        }
    }

    /**
     * 第三方登录
     *
     * @param dto 登录请求（包含平台名 + 授权码）
     * @return 登录结果（JWT Token + 用户信息 + 是否新用户）
     */
    @Transactional
    public OAuthCallbackVO oauthLogin(OAuthLoginDTO dto) {
        String providerName = dto.getProvider().toLowerCase();
        OAuthProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                    "不支持的第三方登录平台: " + providerName);
        }

        // 1. 通过授权码获取第三方用户信息
        OAuthUserInfo oauthInfo = provider.getUserInfo(dto.getCode());

        // 2. 查找是否已有绑定记录
        UserOAuth existingBinding = userOAuthMapper.selectOne(
                new LambdaQueryWrapper<UserOAuth>()
                        .eq(UserOAuth::getProvider, providerName)
                        .eq(UserOAuth::getOpenId, oauthInfo.getOpenId()));

        User user;
        boolean isNewUser = false;

        if (existingBinding != null) {
            // 3a. 已绑定 → 直接登录
            user = userMapper.selectById(existingBinding.getUserId());
            if (user == null) {
                // 用户被删除了，清理绑定记录，重新注册
                userOAuthMapper.deleteById(existingBinding.getId());
                user = registerFromOAuth(oauthInfo);
                isNewUser = true;
            } else {
                // 更新 Token 和第三方信息
                updateOAuthBinding(existingBinding, oauthInfo);
            }
        } else {
            // 3b. 未绑定 → 检查是否可以通过其他方式关联（如 unionId）
            UserOAuth unionBinding = null;
            if (oauthInfo.getUnionId() != null) {
                unionBinding = userOAuthMapper.selectOne(
                        new LambdaQueryWrapper<UserOAuth>()
                                .eq(UserOAuth::getUnionId, oauthInfo.getUnionId())
                                .ne(UserOAuth::getProvider, providerName));
            }

            if (unionBinding != null) {
                // 通过 unionId 关联到已有用户，新增一个平台绑定
                user = userMapper.selectById(unionBinding.getUserId());
                if (user != null) {
                    createOAuthBinding(user.getId(), oauthInfo);
                } else {
                    user = registerFromOAuth(oauthInfo);
                    isNewUser = true;
                }
            } else {
                // 完全新用户 → 自动注册
                user = registerFromOAuth(oauthInfo);
                createOAuthBinding(user.getId(), oauthInfo);
                isNewUser = true;
            }
        }

        // 4. 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 组装返回
        OAuthCallbackVO vo = new OAuthCallbackVO();
        vo.setToken(token);
        vo.setNewUser(isNewUser);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        vo.setUserInfo(userVO);

        log.info("第三方登录成功: provider={}, openId={}, userId={}, isNew={}",
                providerName, oauthInfo.getOpenId(), user.getId(), isNewUser);

        return vo;
    }

    /**
     * 绑定第三方账号到当前用户
     */
    @Transactional
    public void bindOAuth(Long userId, OAuthLoginDTO dto) {
        String providerName = dto.getProvider().toLowerCase();
        OAuthProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                    "不支持的第三方平台: " + providerName);
        }

        // 检查用户是否存在
        if (userMapper.selectById(userId) == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        // 获取第三方用户信息
        OAuthUserInfo oauthInfo = provider.getUserInfo(dto.getCode());

        // 检查该第三方账号是否已被其他用户绑定
        UserOAuth existing = userOAuthMapper.selectOne(
                new LambdaQueryWrapper<UserOAuth>()
                        .eq(UserOAuth::getProvider, providerName)
                        .eq(UserOAuth::getOpenId, oauthInfo.getOpenId()));

        if (existing != null) {
            if (existing.getUserId().equals(userId)) {
                // 已绑定当前用户，更新 Token 即可
                updateOAuthBinding(existing, oauthInfo);
                return;
            }
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                    "该第三方账号已被其他用户绑定");
        }

        // 创建绑定
        createOAuthBinding(userId, oauthInfo);
        log.info("绑定第三方账号: userId={}, provider={}, openId={}",
                userId, providerName, oauthInfo.getOpenId());
    }

    /**
     * 解绑第三方账号
     */
    @Transactional
    public void unbindOAuth(Long userId, String provider) {
        int deleted = userOAuthMapper.delete(
                new LambdaQueryWrapper<UserOAuth>()
                        .eq(UserOAuth::getUserId, userId)
                        .eq(UserOAuth::getProvider, provider.toLowerCase()));

        if (deleted == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "未绑定该第三方平台");
        }

        log.info("解绑第三方账号: userId={}, provider={}", userId, provider);
    }

    /**
     * 查询用户绑定的第三方账号列表
     */
    @Transactional(readOnly = true)
    public List<UserOAuth> getBindings(Long userId) {
        return userOAuthMapper.selectList(
                new LambdaQueryWrapper<UserOAuth>()
                        .eq(UserOAuth::getUserId, userId)
                        .select(UserOAuth::getProvider, UserOAuth::getNickname,
                                UserOAuth::getAvatarUrl, UserOAuth::getCreatedAt));
    }

    // ==================== 私有方法 ====================

    /**
     * 通过第三方信息自动注册新用户
     */
    private User registerFromOAuth(OAuthUserInfo oauthInfo) {
        // 生成唯一用户名（避免冲突）
        String openId = oauthInfo.getOpenId();
        String suffix = openId.length() >= 8 ? openId.substring(0, 8) : openId;
        String baseUsername = oauthInfo.getProvider() + "_" + suffix;
        String username = baseUsername;
        int idx = 0;
        while (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            idx++;
            username = baseUsername + "_" + idx;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(""); // 第三方登录用户无密码
        user.setNickname(oauthInfo.getNickname() != null ? oauthInfo.getNickname() : username);
        user.setAvatarUrl(oauthInfo.getAvatarUrl());
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setPoints(0);
        user.setLevel(1);
        userMapper.insert(user);

        log.info("第三方登录自动注册: userId={}, username={}, provider={}",
                user.getId(), username, oauthInfo.getProvider());

        return user;
    }

    /**
     * 创建 OAuth 绑定记录
     */
    private void createOAuthBinding(Long userId, OAuthUserInfo oauthInfo) {
        UserOAuth binding = new UserOAuth();
        binding.setUserId(userId);
        binding.setProvider(oauthInfo.getProvider());
        binding.setOpenId(oauthInfo.getOpenId());
        binding.setUnionId(oauthInfo.getUnionId());
        binding.setNickname(oauthInfo.getNickname());
        binding.setAvatarUrl(oauthInfo.getAvatarUrl());
        binding.setAccessToken(oauthInfo.getAccessToken());
        binding.setRefreshToken(oauthInfo.getRefreshToken());
        if (oauthInfo.getExpiresIn() != null) {
            binding.setTokenExpiresAt(LocalDateTime.now().plusSeconds(oauthInfo.getExpiresIn()));
        }
        userOAuthMapper.insert(binding);
    }

    /**
     * 更新 OAuth 绑定记录的 Token
     */
    private void updateOAuthBinding(UserOAuth binding, OAuthUserInfo oauthInfo) {
        binding.setAccessToken(oauthInfo.getAccessToken());
        binding.setRefreshToken(oauthInfo.getRefreshToken());
        binding.setNickname(oauthInfo.getNickname());
        binding.setAvatarUrl(oauthInfo.getAvatarUrl());
        if (oauthInfo.getExpiresIn() != null) {
            binding.setTokenExpiresAt(LocalDateTime.now().plusSeconds(oauthInfo.getExpiresIn()));
        }
        userOAuthMapper.updateById(binding);
    }
}
