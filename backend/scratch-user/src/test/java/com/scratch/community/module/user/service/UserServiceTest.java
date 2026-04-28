package com.scratch.community.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.module.user.dto.LoginDTO;
import com.scratch.community.module.user.dto.RegisterDTO;
import com.scratch.community.module.user.entity.User;
import com.scratch.community.module.user.mapper.UserMapper;
import com.scratch.community.module.user.mapper.UserFollowMapper;
import com.scratch.community.common.auth.JwtUtils;
import com.scratch.community.common.util.FileUploadUtils;
import com.scratch.community.module.user.vo.LoginVO;
import com.scratch.community.module.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserFollowMapper userFollowMapper;

    @Mock
    private FileUploadUtils fileUploadUtils;

    @Mock
    private JwtUtils jwtUtils;

    private RegisterDTO validRegisterDTO;

    @BeforeEach
    void setUp() {
        validRegisterDTO = new RegisterDTO();
        validRegisterDTO.setUsername("testuser");
        validRegisterDTO.setPassword("Test123");
        validRegisterDTO.setNickname("测试用户");
        validRegisterDTO.setRole("STUDENT");
    }

    @Nested
    @DisplayName("注册测试")
    class RegisterTests {

        @Test
        @DisplayName("正常注册")
        void register_success() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(1L);
                return 1;
            });
            when(jwtUtils.generateToken(any(), anyString(), anyString())).thenReturn("jwt.token.here");
            when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh.token.here");
            when(jwtUtils.getRefreshTokenExpiry("refresh.token.here")).thenReturn(new java.util.Date(System.currentTimeMillis() + 604800000));

            LoginVO result = userService.register(validRegisterDTO);

            assertNotNull(result);
            assertNotNull(result.getToken());
            assertNotNull(result.getRefreshToken());
            assertEquals("testuser", result.getUserInfo().getUsername());
            verify(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("用户名已存在")
        void register_duplicateUsername() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertThrows(BizException.class, () -> userService.register(validRegisterDTO));
            verify(userMapper, never()).insert(any());
        }

        @Test
        @DisplayName("昵称为空时使用用户名")
        void register_nicknameDefaultsToUsername() {
            validRegisterDTO.setNickname(null);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(1L);
                return 1;
            });
            when(jwtUtils.generateToken(any(), anyString(), anyString())).thenReturn("jwt.token.here");
            when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh.token.here");
            when(jwtUtils.getRefreshTokenExpiry("refresh.token.here")).thenReturn(new java.util.Date(System.currentTimeMillis() + 604800000));

            LoginVO result = userService.register(validRegisterDTO);

            assertEquals("testuser", result.getUserInfo().getNickname());
        }
    }

    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        @Test
        @DisplayName("正常登录")
        void login_success() {
            // 用 BCrypt 生成真实 hash 以便验证密码匹配
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            String hashedPassword = encoder.encode("Test123");

            User user = new User();
            user.setId(1L);
            user.setUsername("testuser");
            user.setPassword(hashedPassword);
            user.setNickname("测试用户");
            user.setRole("STUDENT");
            user.setStatus(1);

            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
            when(jwtUtils.generateToken(eq(1L), eq("testuser"), eq("STUDENT"))).thenReturn("jwt.token.here");
            when(jwtUtils.generateRefreshToken(eq(1L))).thenReturn("refresh.token.here");
            when(jwtUtils.getRefreshTokenExpiry("refresh.token.here")).thenReturn(new java.util.Date(System.currentTimeMillis() + 604800000));

            LoginDTO loginDTO = new LoginDTO();
            loginDTO.setUsername("testuser");
            loginDTO.setPassword("Test123");

            LoginVO result = userService.login(loginDTO);

            assertNotNull(result);
            assertEquals("jwt.token.here", result.getToken());
            assertNotNull(result.getUserInfo());
            assertEquals(1L, result.getUserInfo().getId());
            verify(jwtUtils).generateToken(1L, "testuser", "STUDENT");
        }

        @Test
        @DisplayName("用户不存在")
        void login_userNotFound() {
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            LoginDTO loginDTO = new LoginDTO();
            loginDTO.setUsername("nonexistent");
            loginDTO.setPassword("Test123");

            assertThrows(BizException.class, () -> userService.login(loginDTO));
        }
    }

    @Nested
    @DisplayName("关注测试")
    class FollowTests {

        @Test
        @DisplayName("不能关注自己")
        void follow_self() {
            assertThrows(BizException.class, () -> userService.follow(1L, 1L));
        }

        @Test
        @DisplayName("目标用户不存在")
        void follow_targetNotFound() {
            when(userMapper.selectById(2L)).thenReturn(null);

            assertThrows(BizException.class, () -> userService.follow(1L, 2L));
        }

        @Test
        @DisplayName("已关注则幂等")
        void follow_alreadyFollowed() {
            User target = new User();
            target.setId(2L);
            when(userMapper.selectById(2L)).thenReturn(target);
            // INSERT IGNORE 在唯一约束冲突时返回 0（幂等）
            when(userFollowMapper.insertIgnore(1L, 2L)).thenReturn(0);

            // 不抛异常，幂等返回
            assertDoesNotThrow(() -> userService.follow(1L, 2L));
            verify(userFollowMapper).insertIgnore(1L, 2L);
        }
    }
}
