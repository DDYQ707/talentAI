package com.talent.agent.config;

import feign.Logger;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /** 服务间 Feign 调用时透传网关注入的用户上下文 Header */
    @Bean
    public RequestInterceptor userContextRequestInterceptor() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            String userId = request.getHeader("X-User-Id");
            String userRole = request.getHeader("X-User-Role");
            if (StringUtils.hasText(userId)) {
                template.header("X-User-Id", userId);
            }
            if (StringUtils.hasText(userRole)) {
                template.header("X-User-Role", userRole);
            }
        };
    }
}
