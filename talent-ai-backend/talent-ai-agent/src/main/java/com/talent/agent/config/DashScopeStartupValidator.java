package com.talent.agent.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 启动时校验 DashScope 配置（不输出完整 API Key）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeStartupValidator {

    private final DashScopeProperties dashScopeProperties;

    @PostConstruct
    public void logConfigSummary() {
        String apiKey = dashScopeProperties.getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            log.warn("DashScope API Key 未配置，请设置环境变量 DASHSCOPE_API_KEY");
            return;
        }

        String prefix = apiKey.length() >= 7 ? apiKey.substring(0, 7) + "..." : "***";
        log.info(
                "DashScope 配置已加载: model={}, baseUrl={}, apiKeyLength={}, apiKeyPrefix={}",
                dashScopeProperties.getModel(),
                dashScopeProperties.getBaseUrl(),
                apiKey.length(),
                prefix);

        if (!apiKey.startsWith("sk-")) {
            log.warn("DashScope API Key 格式异常：应以 sk- 开头。请确认使用的是百炼「API-KEY 管理」中的 Key，不是阿里云 AccessKey");
        }
        if (apiKey.contains("${") || apiKey.contains("}")) {
            log.warn("DashScope API Key 疑似未正确解析，当前值包含占位符字符，请检查 Nacos 或 application.yml 配置");
        }
    }
}
