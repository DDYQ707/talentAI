package com.talent.resume.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-job", fallbackFactory = JobFeignClientFallbackFactory.class)
public interface JobFeignClient {

    @GetMapping("/api/job/internal/application/latest-by-resume")
    Map<String, Object> getLatestApplicationByResume(@RequestParam("resumeId") Long resumeId);

    @GetMapping("/api/job/internal/application/latest-by-candidate")
    Map<String, Object> getLatestApplicationByCandidate(@RequestParam("candidateId") Long candidateId);

    @PostMapping("/api/job/internal/application/sync-by-screen-status")
    Map<String, Object> syncApplicationByScreenStatus(@RequestBody Map<String, Object> body);

    @PostMapping("/api/job/internal/application/latest-by-candidates")
    Map<String, Object> getLatestApplicationsByCandidates(@RequestBody Map<String, Object> body);
}
