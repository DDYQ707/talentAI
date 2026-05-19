package com.talent.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class TestController {

    @GetMapping("/ping")
    public String ping() {
        return "恭喜！网关成功将请求路由到了 Auth 认证中心！";
    }
}