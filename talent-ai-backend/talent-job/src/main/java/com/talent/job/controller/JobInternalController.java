package com.talent.job.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.job.entity.JobApplication;
import com.talent.job.service.IJobApplicationService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 微服务内部调用（Feign） */
@RestController
@RequestMapping("/api/job/internal")
@RequiredArgsConstructor
public class JobInternalController {

    private final IJobApplicationService jobApplicationService;

    @GetMapping("/application/latest-by-resume")
    public Map<String, Object> latestApplicationByResume(@RequestParam("resumeId") Long resumeId) {
        Map<String, Object> result = new HashMap<>();
        if (resumeId == null) {
            return result;
        }
        JobApplication app = jobApplicationService.getOne(
                new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getResumeId, resumeId)
                        .orderByDesc(JobApplication::getAppliedAt)
                        .last("LIMIT 1"),
                false);
        return toBrief(app);
    }

    @GetMapping("/application/latest-by-candidate")
    public Map<String, Object> latestApplicationByCandidate(@RequestParam("candidateId") Long candidateId) {
        if (candidateId == null) {
            return Map.of();
        }
        JobApplication app = jobApplicationService.getOne(
                new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getCandidateId, candidateId)
                        .orderByDesc(JobApplication::getAppliedAt)
                        .last("LIMIT 1"),
                false);
        return toBrief(app);
    }

    private Map<String, Object> toBrief(JobApplication app) {
        Map<String, Object> result = new HashMap<>();
        if (app == null) {
            return result;
        }
        result.put("jobTitle", app.getJobTitle());
        result.put("appliedAt", app.getAppliedAt());
        result.put("matchScore", app.getMatchScore());
        result.put("currentStage", app.getCurrentStage());
        result.put("status", app.getStatus());
        return result;
    }
}
