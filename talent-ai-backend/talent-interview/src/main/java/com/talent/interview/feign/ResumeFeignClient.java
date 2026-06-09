package com.talent.interview.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "talent-resume")
public interface ResumeFeignClient {

    @PostMapping("/api/resume/internal/sync-screen-status")
    Map<String, Object> syncScreenStatus(@RequestBody Map<String, Object> body);
}
