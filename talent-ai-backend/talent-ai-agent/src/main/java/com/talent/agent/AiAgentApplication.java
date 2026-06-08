package com.talent.agent;

import com.talent.agent.config.DashScopeProperties;
import com.talent.agent.config.MinioProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties({DashScopeProperties.class, MinioProperties.class})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.talent.agent.client")
@MapperScan("com.talent.agent.mapper")
@EnableAsync
public class AiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }
}
