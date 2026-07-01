package com.talent.interview.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-resume")
public interface ResumeFeignClient {

    @GetMapping("/api/resume/internal/primary-by-candidate")
    Map<String, Object> primaryByCandidate(@RequestParam("candidateId") Long candidateId);

    @PostMapping("/api/resume/internal/sync-screen-status")
    Map<String, Object> syncScreenStatus(@RequestBody Map<String, Object> body);

    @PostMapping("/api/resume/internal/set-screen-status-only")
    Map<String, Object> setScreenStatusOnly(@RequestBody Map<String, Object> body);
}
