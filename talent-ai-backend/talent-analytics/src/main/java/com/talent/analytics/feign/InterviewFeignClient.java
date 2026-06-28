package com.talent.analytics.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "talent-interview", contextId = "analyticsInterviewFeignClient", path = "/api/interview/internal/stats")
public interface InterviewFeignClient {
    @GetMapping("/ongoing-count")
    Long countOngoingInterviews();

    @GetMapping("/completed-this-month")
    Long countCompletedThisMonth();

    @GetMapping("/passed-by-month")
    Long countPassedInterviews(@RequestParam("yearMonth") String yearMonth);

    @GetMapping("/monthly-completed")
    Map<String, Long> countMonthlyCompleted(@RequestParam(value = "months", defaultValue = "6") int months);
}
