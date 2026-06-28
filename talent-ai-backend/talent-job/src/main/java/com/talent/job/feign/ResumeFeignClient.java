package com.talent.job.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-resume")
public interface ResumeFeignClient {

    @GetMapping("/api/resume/brief/{resumeId}")
    Map<String, Object> getResumeBrief(@PathVariable("resumeId") Long resumeId);

    @GetMapping("/api/resume/internal/ai-parse-context")
    Map<String, Object> getAiParseContext(@RequestParam("resumeId") Long resumeId);

    @GetMapping("/api/resume/internal/ownership")
    Map<String, Object> getResumeOwnership(@RequestParam("resumeId") Long resumeId);

    @PostMapping("/api/resume/internal/on-delivery")
    Map<String, Object> markPendingOnDelivery(@RequestBody Map<String, Object> body);

    @PostMapping("/api/resume/internal/set-screen-status-only")
    Map<String, Object> setScreenStatusOnly(@RequestBody Map<String, Object> body);
}
