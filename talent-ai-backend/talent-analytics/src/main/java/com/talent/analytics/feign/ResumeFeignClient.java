package com.talent.analytics.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "talent-resume", path = "/internal/resume/stats")
public interface ResumeFeignClient {
    @GetMapping("/total-count")
    Long getTotalResumeCount();
}