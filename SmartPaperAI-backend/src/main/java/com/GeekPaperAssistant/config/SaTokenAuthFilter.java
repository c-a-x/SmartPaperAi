package com.GeekPaperAssistant.config;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Sa-Token 认证过滤器
 * 用于处理 Bearer Token 格式,将 "Bearer <token>" 转换为 Sa-Token 期望的格式
 *
 * @author 席崇援
 * @since 2025-10-07
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SaTokenAuthFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String authHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);
        
        // 如果存在 Authorization 头且以 "Bearer " 开头,则移除前缀
        if (StrUtil.isNotBlank(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            
            // 创建一个包装器,重写 getHeader 方法
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public String getHeader(String name) {
                    if (AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
                        return token;
                    }
                    return super.getHeader(name);
                }
                
                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
                        return Collections.enumeration(Collections.singletonList(token));
                    }
                    return super.getHeaders(name);
                }
                
                @Override
                public Enumeration<String> getHeaderNames() {
                    return super.getHeaderNames();
                }
            };
            
            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
