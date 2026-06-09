package com.talent.interview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.talent.interview.mapper")
public class TalentInterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalentInterviewApplication.class, args);
    }
}
