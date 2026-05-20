package com.talent.job.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "talent-resume")
public interface ResumeFeignClient {

    @GetMapping("/api/resume/brief/{resumeId}")
    Map<String, Object> getResumeBrief(@PathVariable("resumeId") Long resumeId);
}
