package com.talent.analytics.feign;

import com.talent.analytics.feign.fallback.ResumeFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "talent-resume", contextId = "analyticsResumeFeignClient", path = "/api/resume/internal/stats",
        fallback = ResumeFeignFallback.class)
public interface ResumeFeignClient {
    @GetMapping("/total-count")
    Long getTotalResumeCount();
}