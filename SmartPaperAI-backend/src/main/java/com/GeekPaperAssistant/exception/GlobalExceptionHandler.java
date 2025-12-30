package com.GeekPaperAssistant.exception;

import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.continew.starter.core.exception.BusinessException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 配合 Graceful Response 使用,直接抛出 BusinessException
 *
 * @author 席崇援
 * @since 2025-10-03
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 - 直接抛出,由 Graceful Response 处理
     */
    @ExceptionHandler(BusinessException.class)
    public void handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        throw e;
    }

    /**
     * Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public void handleNotLoginException(NotLoginException e) {
        log.warn("未登录异常: {}", e.getMessage());
        String message = switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未提供认证令牌";
            case NotLoginException.INVALID_TOKEN -> "认证令牌无效";
            case NotLoginException.TOKEN_TIMEOUT -> "认证令牌已过期";
            case NotLoginException.BE_REPLACED -> "您的账号已在其他设备登录";
            case NotLoginException.KICK_OUT -> "您已被强制下线";
            default -> "请先登录";
        };
        throw new BusinessException(message);
    }

    /**
     * 参数校验异常 - @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        throw new BusinessException(message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public void handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定异常: {}", message);
        throw new BusinessException(message);
    }

    /**
     * 其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public void handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        throw new BusinessException("系统异常,请稍后重试");
    }
}
