package com.talent.job;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.talent.job.mapper")
@EnableFeignClients
public class TalentJobApplication {
    public static void main(String[] args) {
        SpringApplication.run(TalentJobApplication.class, args);
    }
}
