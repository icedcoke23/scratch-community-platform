package com.scratch.community.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scratch.community.module.user.entity.UserOAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户第三方登录绑定 Mapper
 */
@Mapper
public interface UserOAuthMapper extends BaseMapper<UserOAuth> {
}
