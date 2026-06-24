package com.talent.analytics.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "talent-job", path = "/internal/job/stats")
public interface JobFeignClient {

    @GetMapping("/active-job-count")
    Long countActiveJobs();

    @GetMapping("/application-count-by-stage")
    Map<Integer, Long> countApplicationsByStage(@RequestParam("stages") List<Integer> stages);

    @GetMapping("/application-count-by-status")
    Map<Integer, Long> countApplicationsByStatus(@RequestParam("statuses") List<Integer> statuses);
}