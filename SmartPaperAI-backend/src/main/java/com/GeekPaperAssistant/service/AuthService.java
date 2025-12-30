package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.dto.LoginRequest;
import com.GeekPaperAssistant.model.dto.RegisterRequest;
import com.GeekPaperAssistant.model.vo.CaptchaVO;
import com.GeekPaperAssistant.model.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证服务接口
 *
 * @author 席崇援
 */
public interface AuthService {

    /**
     * 获取验证码
     *
     * @return 验证码信息
     */
    CaptchaVO getCaptcha();

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @param request HTTP请求对象，用于获取客户端IP
     * @return 登录返回信息
     */
    LoginVO login(LoginRequest loginRequest, HttpServletRequest request);

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     */
    void register(RegisterRequest registerRequest);

    /**
     * 退出登录
     */
    void logout();

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    Long getCurrentUserId();

    /**
     * 检查用户是否已登录
     *
     * @return 是否已登录
     */
    boolean isLogin();
}
