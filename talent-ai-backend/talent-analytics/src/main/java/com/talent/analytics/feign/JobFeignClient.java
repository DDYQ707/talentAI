package com.talent.analytics.feign;

import com.talent.analytics.feign.dto.OfferStatsDTO;
import com.talent.analytics.feign.dto.MonthlyCountDTO;
import com.talent.analytics.feign.dto.DepartmentJobStatDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "talent-job", contextId = "analyticsJobFeignClient", path = "/api/job/internal/stats")
public interface JobFeignClient {

    @GetMapping("/active-job-count")
    Long countActiveJobs();

    @GetMapping("/monthly-application-count")
    Long countMonthlyApplications();

    @GetMapping("/application-count-by-stage")
    Map<Integer, Long> countApplicationsByStage(@RequestParam("stages") List<Integer> stages);

    @GetMapping("/application-count-by-status")
    Map<Integer, Long> countApplicationsByStatus(@RequestParam("statuses") List<Integer> statuses);

    @GetMapping("/offer-metrics")
    OfferStatsDTO getOfferMetrics();

    @GetMapping("/monthly-applications")
    List<MonthlyCountDTO> getMonthlyApplications(@RequestParam(defaultValue = "6") int months);

    @GetMapping("/monthly-offers")
    List<MonthlyCountDTO> getMonthlyOffers(@RequestParam(defaultValue = "6") int months);

    @GetMapping("/department-job-stats")
    List<DepartmentJobStatDTO> getDepartmentJobStats();
}