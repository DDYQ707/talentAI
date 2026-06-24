package com.talent.analytics.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-interview", path = "/internal/interview/stats")
public interface InterviewFeignClient {
    @GetMapping("/ongoing-count")
    Long countOngoingInterviews();

    @GetMapping("/passed-by-month")
    Long countPassedInterviews(@RequestParam("yearMonth") String yearMonth);
}