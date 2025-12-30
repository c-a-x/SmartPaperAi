package com.GeekPaperAssistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 *
 * @author 席崇援
 * @since 2025-10-03
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码
     */
    @Schema(description = "验证码", example = "abcd")
    private String captcha;

    /**
     * 验证码key
     */
    @Schema(description = "验证码key")
    private String captchaKey;
}
