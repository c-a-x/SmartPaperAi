package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码返回信息
 *
 * @author 席崇援
 * @since 2025-10-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码返回信息")
public class CaptchaVO {

    /**
     * 验证码key
     */
    @Schema(description = "验证码key")
    private String captchaKey;

    /**
     * 验证码图片(Base64)
     */
    @Schema(description = "验证码图片(Base64)")
    private String captchaImage;

    /**
     * 过期时间(秒)
     */
    @Schema(description = "过期时间(秒)")
    private Long expireTime;
}
