package com.talent.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置（用于探测 Nacos OpenAPI）
 *
 * @author TalentAI
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate dashboardRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时 2s，读取超时 3s，避免 Nacos 不可用时拖垮大屏接口
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        return new RestTemplate(factory);
    }
}