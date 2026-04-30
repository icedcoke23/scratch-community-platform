package com.scratch.community.common.exception;

import com.scratch.community.common.result.ErrorCode;
import com.scratch.community.common.result.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 *
 * <p>异常处理优先级:
 * <ol>
 *   <li>BizException — 业务异常，直接返回业务错误码</li>
 *   <li>Validation — 参数校验异常，返回 400 + PARAM_ERROR</li>
 *   <li>NoResourceFound — 404 资源不存在</li>
 *   <li>Exception — 兜底，返回 9999 系统异常（不泄露内部信息）</li>
 * </ol>
 *
 * <p>安全原则: 生产环境绝不暴露堆栈信息、SQL 语句或内部路径给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private boolean isProd() {
        return "prod".equals(activeProfile) || "production".equals(activeProfile);
    }

    /**
     * 业务异常 — 已知的业务错误，直接返回
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<?> handleBizException(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常 — @Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.debug("参数校验失败: {}", message);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 参数绑定异常 — @ModelAttribute 绑定失败
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数绑定失败");
        log.debug("参数绑定失败: {}", message);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 约束违反异常 — @PathVariable / @RequestParam 校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleConstraintViolation(ConstraintViolationException e) {
        log.debug("约束违反: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 404 资源不存在 — 返回友好的错误信息
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> handleNoResourceFound(NoResourceFoundException e) {
        return R.fail(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 405 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.debug("请求方法不支持: {} {}", e.getMethod(), e.getMessage());
        return R.fail(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 415 不支持的媒体类型
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public R<?> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        MediaType contentType = e.getContentType();
        String typeName = contentType != null ? contentType.toString() : "unknown";
        log.debug("不支持的媒体类型: {}", typeName);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), "不支持的媒体类型: " + typeName);
    }

    /**
     * 兜底异常 — 所有未处理的异常
     * <p>安全: 只记录日志，不暴露内部信息给前端。生产环境隐藏异常类名。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleException(Exception e) {
        // 区分异常类型，优化日志级别
        if (e instanceof NullPointerException || e instanceof ClassCastException) {
            log.error("系统异常 [编程错误]: {}", e.getClass().getSimpleName(), e);
        } else {
            log.error("系统异常 [运行时]: {}", e.getMessage(), e);
        }
        // 生产环境: 不泄露任何内部信息; 开发环境: 返回异常类名辅助调试
        if (isProd()) {
            return R.fail(ErrorCode.SYSTEM_ERROR);
        }
        return R.fail(ErrorCode.SYSTEM_ERROR.getCode(),
                ErrorCode.SYSTEM_ERROR.getMsg() + " [" + e.getClass().getSimpleName() + "]");
    }
}
