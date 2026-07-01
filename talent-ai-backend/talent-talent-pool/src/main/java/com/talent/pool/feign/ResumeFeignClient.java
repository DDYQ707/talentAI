package com.talent.pool.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "talent-resume")
public interface ResumeFeignClient {

    @GetMapping("/api/resume/internal/hr/{resumeId}/brief")
    Map<String, Object> hrResumeBrief(@PathVariable("resumeId") Long resumeId);
}
