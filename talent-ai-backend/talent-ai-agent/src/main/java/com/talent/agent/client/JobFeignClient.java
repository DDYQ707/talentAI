package com.talent.agent.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-job")
public interface JobFeignClient {

    @GetMapping("/api/job/internal/application/latest-by-resume")
    Map<String, Object> getLatestApplicationByResume(@RequestParam("resumeId") Long resumeId);

    @GetMapping("/api/job/internal/application/by-id")
    Map<String, Object> getApplicationById(@RequestParam("applicationId") Long applicationId);

    @GetMapping("/api/job/internal/post/brief")
    Map<String, Object> getJobPostBrief(@RequestParam("jobId") Long jobId);

    @PostMapping("/api/job/internal/application/sync-match-score")
    Map<String, Object> syncApplicationMatchScore(@RequestBody Map<String, Object> body);
}
