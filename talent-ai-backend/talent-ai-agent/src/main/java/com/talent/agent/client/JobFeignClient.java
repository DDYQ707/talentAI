package com.talent.agent.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-job")
public interface JobFeignClient {

    @GetMapping("/api/job/internal/application/latest-by-resume")
    Map<String, Object> getLatestApplicationByResume(@RequestParam("resumeId") Long resumeId);
}
