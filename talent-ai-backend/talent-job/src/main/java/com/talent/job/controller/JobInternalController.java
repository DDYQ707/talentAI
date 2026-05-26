package com.talent.job.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.job.dto.BatchCandidateIdsRequest;
import com.talent.job.dto.SyncScreenStatusRequest;
import com.talent.job.entity.JobApplication;
import com.talent.job.service.IJobApplicationService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** HR 改 resume.screen_status 时，同步最近一条投递的阶段与状态 */
    @PostMapping("/application/sync-by-screen-status")
    public Map<String, Object> syncByScreenStatus(@RequestBody SyncScreenStatusRequest request) {
        return jobApplicationService.syncLatestApplicationByScreenStatus(request);
    }

    /** 批量查询各候选人最近一条投递（HR 简历列表） */
    @PostMapping("/application/latest-by-candidates")
    public Map<String, Object> latestByCandidates(@RequestBody BatchCandidateIdsRequest request) {
        Map<String, Map<String, Object>> items = new LinkedHashMap<>();
        if (request == null || request.getCandidateIds() == null) {
            return Map.of("items", items);
        }
        for (Long candidateId : request.getCandidateIds()) {
            if (candidateId == null) {
                continue;
            }
            JobApplication app = jobApplicationService.getOne(
                    new LambdaQueryWrapper<JobApplication>()
                            .eq(JobApplication::getCandidateId, candidateId)
                            .orderByDesc(JobApplication::getAppliedAt)
                            .last("LIMIT 1"),
                    false);
            items.put(String.valueOf(candidateId), toBrief(app));
        }
        return Map.of("items", items);
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
