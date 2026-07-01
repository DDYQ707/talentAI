package com.talent.admin.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "talent-job", contextId = "adminJobFeignClient")
public interface JobAdminFeignClient {

    @PutMapping("/api/job/internal/admin/takedown")
    Map<String, Object> adminTakedown(@RequestBody Map<String, Object> body);
}
