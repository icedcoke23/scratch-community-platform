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
 * QQ 登录提供者
 *
 * QQ 登录流程:
 * 1. 前端引导用户跳转到 QQ 授权页
 * 2. 用户授权后，QQ 重定向到回调地址并携带 code
 * 3. 后端用 code 换取 access_token
 * 4. 用 access_token 获取 openid（QQ 的 OpenID 获取是独立接口）
 * 5. 用 access_token + openid 获取用户信息
 *
 * 文档: https://wiki.connect.qq.com/%E5%BC%80%E5%8F%91%E6%94%BB%E7%95%A5_server-side
 */
@Slf4j
@Component
public class QQOAuthProvider implements OAuthProvider {

    private static final String TOKEN_URL =
            "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s&fmt=json";
    private static final String OPEN_ID_URL =
            "https://graph.qq.com/oauth2.0/me?access_token=%s&fmt=json";
    private static final String USER_INFO_URL =
            "https://graph.qq.com/user/get_user_info?access_token=%s&oauth_consumer_key=%s&openid=%s";

    @Value("${oauth.qq.app-id:}")
    private String appId;

    @Value("${oauth.qq.app-secret:}")
    private String appSecret;

    @Value("${oauth.qq.redirect-uri:}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() {
        return "qq";
    }

    @Override
    public OAuthUserInfo getUserInfo(String code) {
        try {
            // 1. 用 code 换取 access_token
            String tokenUrl = String.format(TOKEN_URL, appId, appSecret, code, redirectUri);
            ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenUrl, String.class);
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());

            if (tokenJson.has("error")) {
                String errMsg = tokenJson.has("error_description") ?
                        tokenJson.get("error_description").asText() : "获取 Token 失败";
                log.error("QQ 获取 token 失败: {}", errMsg);
                throw new BizException(ErrorCode.THIRD_PARTY_AUTH_FAILED.getCode(), "QQ 登录失败: " + errMsg);
            }

            String accessToken = tokenJson.get("access_token").asText();
            int expiresIn = tokenJson.has("expires_in") ? tokenJson.get("expires_in").asInt() : 7200;

            // 2. 获取 OpenID
            String openIdUrl = String.format(OPEN_ID_URL, accessToken);
            ResponseEntity<String> openIdResponse = restTemplate.getForEntity(openIdUrl, String.class);
            String openIdBody = openIdResponse.getBody();

            // QQ 返回格式: callback( {"client_id":"...","openid":"..."} );
            // 需要提取 JSON 部分
            String jsonStr = openIdBody;
            if (jsonStr.contains("callback")) {
                int start = jsonStr.indexOf('{');
                int end = jsonStr.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    jsonStr = jsonStr.substring(start, end + 1);
                }
            }

            JsonNode openIdJson = objectMapper.readTree(jsonStr);
            if (openIdJson.has("error")) {
                throw new BizException(ErrorCode.THIRD_PARTY_AUTH_FAILED.getCode(), "QQ 获取 OpenID 失败");
            }

            String openId = openIdJson.get("openid").asText();

            // 3. 获取用户信息
            String userInfoUrl = String.format(USER_INFO_URL, accessToken, appId, openId);
            ResponseEntity<String> userInfoResponse = restTemplate.getForEntity(userInfoUrl, String.class);
            JsonNode userInfoJson = objectMapper.readTree(userInfoResponse.getBody());

            if (userInfoJson.has("ret") && userInfoJson.get("ret").asInt() != 0) {
                throw new BizException(ErrorCode.THIRD_PARTY_AUTH_FAILED.getCode(), "QQ 获取用户信息失败");
            }

            // 4. 组装返回
            OAuthUserInfo info = new OAuthUserInfo();
            info.setOpenId(openId);
            info.setProvider("qq");
            info.setNickname(userInfoJson.has("nickname") ? userInfoJson.get("nickname").asText() : "QQ 用户");
            info.setAvatarUrl(userInfoJson.has("figureurl_qq_2") ?
                    userInfoJson.get("figureurl_qq_2").asText() :
                    (userInfoJson.has("figureurl_qq_1") ? userInfoJson.get("figureurl_qq_1").asText() : null));
            info.setAccessToken(accessToken);
            info.setExpiresIn(expiresIn);

            return info;

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("QQ 登录异常", e);
            throw new BizException(ErrorCode.THIRD_PARTY_AUTH_FAILED.getCode(), "QQ 登录异常: " + e.getMessage());
        }
    }
}
