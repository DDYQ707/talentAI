package com.talent.agent.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-resume")
public interface ResumeFeignClient {

    @GetMapping("/api/resume/brief/{resumeId}")
    Map<String, Object> getResumeBrief(@PathVariable("resumeId") Long resumeId);

    @GetMapping("/api/resume/internal/ownership")
    Map<String, Object> getResumeOwnership(@RequestParam("resumeId") Long resumeId);
}
