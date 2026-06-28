package com.talent.pool.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-auth")
public interface AuthFeignClient {

    @GetMapping("/api/auth/internal/candidateBrief")
    Map<String, Object> candidateBrief(@RequestParam("userId") Long userId);
}
