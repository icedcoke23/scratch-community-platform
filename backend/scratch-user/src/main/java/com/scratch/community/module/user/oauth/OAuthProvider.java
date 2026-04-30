package com.scratch.community.module.user.oauth;

/**
 * 第三方登录提供者接口
 * 各平台（微信/QQ）实现此接口
 */
public interface OAuthProvider {

    /**
     * 获取平台标识
     */
    String getProviderName();

    /**
     * 通过授权码获取用户信息
     *
     * @param code 第三方平台返回的授权码
     * @return 用户信息
     */
    OAuthUserInfo getUserInfo(String code);
}
