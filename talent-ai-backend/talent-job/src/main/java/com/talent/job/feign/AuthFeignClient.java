package com.talent.job.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

// 这个 name 就是你要呼叫的微服务在 Nacos 里的名字
@FeignClient(name = "talent-auth")
public interface AuthFeignClient {

    // 把在 AuthController 里写的接口签名一模一样地抄过来
    @GetMapping("/api/auth/getUserName")
    String getUserName(@RequestParam("id") Long id);

    /** 候选人档案是否已完善（投递前校验） */
    @GetMapping("/api/auth/candidate/profile/complete")
    java.util.Map<String, Object> getProfileCompleteness(@RequestHeader("X-User-Id") Long userId);

    /** 创建系统通知（微服务内部） */
    @org.springframework.web.bind.annotation.PostMapping("/api/auth/notification/internal/create")
    java.util.Map<String, Object> createNotification(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> body);

}