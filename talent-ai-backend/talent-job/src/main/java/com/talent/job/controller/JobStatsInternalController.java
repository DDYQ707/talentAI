package com.talent.job.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.job.constant.JobApplicationConstants;
import com.talent.job.entity.JobApplication;
import com.talent.job.entity.JobPost;
import com.talent.job.service.IJobApplicationService;
import com.talent.job.service.IJobPostService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 供 talent-analytics 聚合统计（Feign 内部调用） */
@RestController
@RequestMapping("/api/job/internal/stats")
@RequiredArgsConstructor
public class JobStatsInternalController {

    private final IJobPostService jobPostService;
    private final IJobApplicationService jobApplicationService;

    @GetMapping("/active-job-count")
    public Long countActiveJobs() {
        return jobPostService.count(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getStatus, JobApplicationConstants.JOB_STATUS_OPEN));
    }

    @GetMapping("/monthly-application-count")
    public Long countMonthlyApplications() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return jobApplicationService.count(new LambdaQueryWrapper<JobApplication>()
                .ge(JobApplication::getAppliedAt, monthStart));
    }

    @GetMapping("/application-count-by-stage")
    public Map<Integer, Long> countApplicationsByStage(@RequestParam("stages") List<Integer> stages) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        if (stages == null || stages.isEmpty()) {
            return result;
        }
        for (Integer stage : stages) {
            if (stage == null) {
                continue;
            }
            long count = jobApplicationService.count(new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getCurrentStage, stage.byteValue()));
            result.put(stage, count);
        }
        return result;
    }

    @GetMapping("/application-count-by-status")
    public Map<Integer, Long> countApplicationsByStatus(@RequestParam("statuses") List<Integer> statuses) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        if (statuses == null || statuses.isEmpty()) {
            return result;
        }
        for (Integer status : statuses) {
            if (status == null) {
                continue;
            }
            long count = jobApplicationService.count(new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getStatus, status.byteValue()));
            result.put(status, count);
        }
        return result;
    }
}
