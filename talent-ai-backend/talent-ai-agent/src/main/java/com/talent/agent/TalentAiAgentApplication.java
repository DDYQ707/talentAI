package com.talent.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TalentAiAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(TalentAiAgentApplication.class, args);
    }
}
