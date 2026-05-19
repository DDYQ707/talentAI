package com.talent.job.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 这个 name 就是你要呼叫的微服务在 Nacos 里的名字
@FeignClient(name = "talent-auth")
public interface AuthFeignClient {

    // 把在 AuthController 里写的接口签名一模一样地抄过来
    @GetMapping("/api/auth/getUserName")
    String getUserName(@RequestParam("id") Long id);

}