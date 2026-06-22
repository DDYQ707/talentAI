package com.talent.pool;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.talent.pool.mapper")
public class TalentPoolApplication {
    public static void main(String[] args) {
        SpringApplication.run(TalentPoolApplication.class, args);
    }
}
