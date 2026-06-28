package com.talent.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * talent-admin 后台管理微服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.talent.admin.mapper")
public class TalentAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalentAdminApplication.class, args);
    }
}