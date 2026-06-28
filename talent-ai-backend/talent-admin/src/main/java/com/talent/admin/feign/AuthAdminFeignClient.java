package com.talent.admin.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "talent-auth", contextId = "adminAuthFeignClient")
public interface AuthAdminFeignClient {

    @PostMapping("/api/auth/notification/internal/broadcast")
    Map<String, Object> broadcastAnnouncement(@RequestBody Map<String, Object> body);
}
