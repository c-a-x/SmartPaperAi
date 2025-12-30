package com.GeekPaperAssistant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import com.GeekPaperAssistant.model.dto.LoginRequest;
import com.GeekPaperAssistant.model.dto.RegisterRequest;
import com.GeekPaperAssistant.model.vo.CaptchaVO;
import com.GeekPaperAssistant.model.vo.LoginVO;
import com.GeekPaperAssistant.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author 席崇援
 * @since 2025-10-03
 */
@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 获取验证码
     *
     * @return 验证码信息
     */
    @SaIgnore
    @GetMapping("/captcha")
    @Operation(summary = "获取验证码", description = "获取图形验证码")
    public CaptchaVO getCaptcha() {
        return authService.getCaptcha();
    }

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @param request HTTP请求对象
     * @return 登录返回信息
     */
    @SaIgnore
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户通过用户名和密码进行登录")
    public LoginVO login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        return authService.login(loginRequest, request);
    }

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     */
    @SaIgnore
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册账号")
    public void register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
    }

    /**
     * 退出登录
     */
    @SaCheckLogin
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "用户退出当前登录状态")
    public void logout() {
        authService.logout();
    }

    /**
     * 检查登录状态
     *
     * @return 登录状态
     */
    @SaIgnore
    @GetMapping("/check")
    @Operation(summary = "检查登录状态", description = "检查用户当前是否处于登录状态")
    public Boolean checkLogin() {
        return authService.isLogin();
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    @SaCheckLogin
    @GetMapping("/current-user-id")
    @Operation(summary = "获取当前用户ID", description = "获取当前登录用户的ID")
    public Long getCurrentUserId() {
        return authService.getCurrentUserId();
    }
}
