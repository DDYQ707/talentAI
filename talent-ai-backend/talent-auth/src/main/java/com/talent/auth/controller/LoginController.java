package com.talent.auth.controller;

import com.talent.common.core.domain.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    /**
     * 模拟登录接口
     * 注意：因为我们在网关配置了 StripPrefix=1，网关会把 /api/auth/login 截断成 /login 转发过来
     */
    @PostMapping("/login")
    public Result<String> login(@RequestParam String username, @RequestParam String password) {
        // 这里只是模拟，后面我们会接入真正的数据库和 JWT Token 生成
        if ("admin".equals(username) && "123456".equals(password)) {
            return Result.success("模拟生成的Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        } else {
            return Result.error("账号或密码错误，请重试");
        }
    }
}