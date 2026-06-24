package com.talent.interview.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-auth")
public interface AuthFeignClient {

    @GetMapping("/api/auth/internal/user/brief")
    Map<String, Object> userBrief(@RequestParam("userId") Long userId);

    @PostMapping("/api/auth/notification/internal/create")
    Map<String, Object> createNotification(@RequestBody Map<String, Object> body);
}
