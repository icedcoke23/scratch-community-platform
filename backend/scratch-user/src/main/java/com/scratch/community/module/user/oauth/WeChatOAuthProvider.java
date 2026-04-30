package com.scratch.community.module.user.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scratch.community.common.exception.BizException;
import com.scratch.community.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

/**
 * 微信登录提供者
 *
 * 微信网页登录流程:
 * 1. 前端引导用户跳转到微信授权页
 * 2. 用户授权后，微信重定向到回调地址并携带 code
 * 3. 后端用 code 换取 access_token + openid
 * 4. 用 access_token + openid 获取用户信息
 *
 * 文档: https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html
 */
@Slf4j
@Component
public class WeChatOAuthProvider implements OAuthProvider {

    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
    private static final String USER_INFO_URL =
            "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN";
    private static final String REFRESH_TOKEN_URL =
            "https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=%s&grant_type=refresh_token&refresh_token=%s";

    @Value("${oauth.wechat.app-id:}")
    private String appId;

    @Value("${oauth.wechat.app-secret:}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() {
        return "wechat";
    }

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        try {
            // 1. 用 code 换取 access_token
            String tokenUrl = String.format(TOKEN_URL, appId, appSecret, code);
            ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenUrl, String.class);
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());

            if (tokenJson.has("errcode") && tokenJson.get("errcode").asInt() != 0) {
                String errMsg = tokenJson.has("errmsg") ? tokenJson.get("errmsg").asText() : "未知错误";
                log.error("微信获取 token 失败: {}", errMsg);
                throw new BizException(ErrorCode.THIRD_PARTY_AUTH_FAILED.getCode(), "微信登录失败: " + errMsg);
            }

            String accessToken = tokenJson.get("access_token").asText();
            String openId = tokenJson.get("openid").asText();
            String unionId = tokenJson.has("unionid") ? tokenJson.get("unionid").asText() : null;
            String refreshToken = tokenJson.has("refresh_token") ? tokenJson.get("refresh_token").asText() : null;
            int expiresIn = tokenJson.has("expires_in") ? tokenJson.get("expires_in").asInt() : 7200;

            // 2. 获取用户信息
            String userInfoUrl = String.format(USER_INFO_URL, accessToken, openId);
            ResponseEntity<String> userInfoResponse = restTemplate.getForEntity(userInfoUrl, String.class);
            JsonNode userInfoJson = objectMapper.readTree(userInfoResponse.getBody());

            if (userInfoJson.has("errcode") && userInfoJson.get("errcode").asInt() != 0) {
                log.error("微信获取用户信息失败: {}", userInfoJson);
                throw new BizException(ErrorCode.THIRD_PARTY_AUTH_FAILED.getCode(), "微信获取用户信息失败");
            }

            // 3. 组装返回
            OAuthUserInfo info = new OAuthUserInfo();
            info.setOpenId(openId);
            info.setUnionId(unionId);
            info.setProvider("wechat");
            info.setNickname(userInfoJson.has("nickname") ? userInfoJson.get("nickname").asText() : "微信用户");
            info.setAvatarUrl(userInfoJson.has("headimgurl") ? userInfoJson.get("headimgurl").asText() : null);
            info.setAccessToken(accessToken);
            info.setRefreshToken(refreshToken);
            info.setExpiresIn(expiresIn);

            return info;

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信登录异常", e);
            throw new BizException(ErrorCode.THIRD_PARTY_AUTH_FAILED.getCode(), "微信登录异常: " + e.getMessage());
        }
    }

    /**
     * 刷新 Access Token
     */
    public OAuthUserInfo refreshToken(String refreshToken) {
        try {
            String url = String.format(REFRESH_TOKEN_URL, appId, refreshToken);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());

            if (json.has("errcode") && json.get("errcode").asInt() != 0) {
                throw new BizException(ErrorCode.THIRD_PARTY_AUTH_FAILED.getCode(), "刷新微信 Token 失败");
            }

            OAuthUserInfo info = new OAuthUserInfo();
            info.setOpenId(json.get("openid").asText());
            info.setUnionId(json.has("unionid") ? json.get("unionid").asText() : null);
            info.setAccessToken(json.get("access_token").asText());
            info.setRefreshToken(json.get("refresh_token").asText());
            info.setExpiresIn(json.get("expires_in").asInt());
            info.setProvider("wechat");

            return info;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("刷新微信 Token 异常", e);
            throw new BizException(ErrorCode.THIRD_PARTY_AUTH_FAILED.getCode(), "刷新 Token 失败");
        }
    }
}
