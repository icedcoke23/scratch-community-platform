package com.scratch.community.common.idempotent;

import java.lang.annotation.*;

/**
 * 接口幂等性注解
 *
 * <p>标记在 Controller 方法上，表示该接口需要幂等性保护。
 * 客户端需在请求头中传递 {@code X-Idempotent-Key}，服务端通过 Redis 去重。
 *
 * <p>工作原理：
 * <ol>
 *   <li>首次请求：用 Redis {@code SET key NX EX 300} 锁定 Key，正常执行并缓存响应</li>
 *   <li>重复请求：检测到 Key 已存在，直接返回缓存的响应</li>
 *   <li>Key 过期时间 5 分钟，防止 Redis 内存泄漏</li>
 * </ol>
 *
 * <p>使用示例：
 * <pre>
 * &#64;Idempotent
 * &#64;PostMapping("/like")
 * public R&lt;Void&gt; like(...) { ... }
 * </pre>
 *
 * @author scratch-community
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
}
