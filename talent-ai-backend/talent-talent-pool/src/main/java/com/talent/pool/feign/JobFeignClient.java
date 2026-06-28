package com.talent.pool.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-job")
public interface JobFeignClient {

    @GetMapping("/api/job/internal/application/by-id")
    Map<String, Object> applicationById(@RequestParam("applicationId") Long applicationId);
}
