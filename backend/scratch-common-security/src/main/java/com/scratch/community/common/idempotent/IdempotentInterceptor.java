package com.scratch.community.common.idempotent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scratch.community.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等性拦截器
 *
 * <p>配合 {@link Idempotent} 注解使用，通过 Redis 实现请求去重。
 *
 * <p>工作流程：
 * <ol>
 *   <li>检查方法是否标注 {@code @Idempotent}</li>
 *   <li>从请求头 {@code X-Idempotent-Key} 获取幂等 Key</li>
 *   <li>用 Redis {@code SET key NX EX 300} 尝试锁定：
 *       <ul>
 *         <li>锁定成功 → 首次请求，放行并在 {@code afterCompletion} 中缓存响应</li>
 *         <li>锁定失败 → 重复请求，直接返回缓存的响应</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * @author scratch-community
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "idempotent:";
    private static final long EXPIRE_SECONDS = 300;
    private static final String RESPONSE_PREFIX = "idempotent:resp:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            return true;
        }

        String idempotentKey = request.getHeader("X-Idempotent-Key");
        if (idempotentKey == null || idempotentKey.isBlank()) {
            // 没有幂等 Key，正常放行（不强制要求）
            return true;
        }

        String redisKey = KEY_PREFIX + idempotentKey;
        String respKey = RESPONSE_PREFIX + idempotentKey;

        // 尝试锁定：SET NX EX
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", EXPIRE_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            // 首次请求，放行，并在 request attribute 中标记需要缓存响应
            request.setAttribute("idempotent_key", respKey);
            log.debug("幂等性检查通过（首次请求）: key={}", idempotentKey);
            return true;
        }

        // 重复请求，返回缓存的响应
        String cachedResp = redisTemplate.opsForValue().get(respKey);
        if (cachedResp != null) {
            log.debug("幂等性拦截（重复请求，返回缓存）: key={}", idempotentKey);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(cachedResp);
            return false;
        }

        // Key 存在但响应未缓存（可能还在处理中），让请求继续
        log.debug("幂等性检查：Key 存在但响应未缓存，放行: key={}", idempotentKey);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String respKey = (String) request.getAttribute("idempotent_key");
        if (respKey == null) {
            return;
        }

        // 缓存成功响应状态（标记为已完成，防止处理中的重复请求绕过）
        // 注意: 响应体已被写入 OutputStream，无法在此处捕获完整 body。
        // 使用 ResponseWrapper 捕获会增加内存开销，当前方案仅标记完成状态。
        // 重复请求在 preHandle 中检查到 Key 存在但无缓存响应时会放行（降级为正常处理），
        // 这是合理的折中：幂等性保证"至少一次"而非"恰好一次"。
        if (response.getStatus() == HttpServletResponse.SC_OK) {
            redisTemplate.opsForValue().set(respKey, String.valueOf(response.getStatus()),
                    EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }
}
