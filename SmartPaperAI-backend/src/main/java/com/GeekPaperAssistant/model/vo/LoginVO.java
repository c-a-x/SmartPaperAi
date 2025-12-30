package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录返回信息
 *
 * @author 席崇援
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录返回信息")
public class LoginVO {

    /**
     * Token
     */
    @Schema(description = "访问令牌")
    private String token;

    /**
     * Token 名称
     */
    @Schema(description = "Token名称")
    private String tokenName;

    /**
     * Token 前缀
     */
    @Schema(description = "Token前缀")
    private String tokenPrefix;

    /**
     * Token 有效期(秒)
     */
    @Schema(description = "Token有效期(秒)")
    private Long tokenTimeout;

    /**
     * 用户信息
     */
    @Schema(description = "用户信息")
    private UserInfoVO userInfo;
}
