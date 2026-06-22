package com.talent.agent.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-interview")
public interface InterviewFeignClient {

    @GetMapping("/api/interview/internal/by-application")
    Map<String, Object> listByApplication(@RequestParam("applicationId") Long applicationId);

    @GetMapping("/api/interview/internal/{interviewId}/brief")
    Map<String, Object> getBrief(@PathVariable("interviewId") Long interviewId);

    @GetMapping("/api/interview/internal/evaluations-by-application")
    Map<String, Object> listEvaluationsByApplication(@RequestParam("applicationId") Long applicationId);
}
