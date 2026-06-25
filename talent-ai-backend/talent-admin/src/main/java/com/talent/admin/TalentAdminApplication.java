package com.talent.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * talent-admin 后台管理微服务启动类
 *
 * @author TalentAI
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.talent.admin.mapper")
public class TalentAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalentAdminApplication.class, args);
    }
}