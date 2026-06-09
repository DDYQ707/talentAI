package com.talent.interview.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-job")
public interface JobFeignClient {

    @GetMapping("/api/job/internal/application/by-id")
    Map<String, Object> applicationById(@RequestParam("applicationId") Long applicationId);

    @PostMapping("/api/job/internal/application/sync-by-screen-status")
    Map<String, Object> syncByScreenStatus(@RequestBody Map<String, Object> body);
}
