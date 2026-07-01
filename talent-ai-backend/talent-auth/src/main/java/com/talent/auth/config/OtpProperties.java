package com.talent.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@RefreshScope
@ConfigurationProperties(prefix = "talent.auth.otp")
public class OtpProperties {

    /** 验证码有效时长（秒） */
    private int expireSeconds = 300;

    /** 同一账号发送间隔（秒） */
    private int sendCooldownSeconds = 60;

    /** 开发环境是否在响应中返回验证码（生产务必 false） */
    private boolean devExpose = true;
}
