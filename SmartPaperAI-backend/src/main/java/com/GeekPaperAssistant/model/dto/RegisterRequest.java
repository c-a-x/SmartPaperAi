package com.GeekPaperAssistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求
 *
 * @author 席崇援
 * @since 2025-10-03
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "testuser")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度为4-20个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20个字符")
    private String password;

    /**
     * 确认密码
     */
    @Schema(description = "确认密码", example = "123456")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "user@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

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
