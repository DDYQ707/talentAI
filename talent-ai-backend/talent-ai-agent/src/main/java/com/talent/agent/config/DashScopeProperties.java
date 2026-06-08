package com.talent.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai.dashscope")
public class DashScopeProperties {

    private String apiKey;
    private String baseUrl;
    private String model;
    private int timeoutSeconds = 60;
}
