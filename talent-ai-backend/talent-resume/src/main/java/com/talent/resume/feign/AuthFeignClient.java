package com.talent.resume.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-auth")
public interface AuthFeignClient {

    @GetMapping("/api/auth/getUserName")
    String getUserName(@RequestParam("id") Long id);
}
