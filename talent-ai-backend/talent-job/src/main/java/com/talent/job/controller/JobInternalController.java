package com.talent.job.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.job.dto.BatchCandidateIdsRequest;
import com.talent.job.dto.SyncScreenStatusRequest;
import com.talent.job.entity.JobApplication;
import com.talent.job.entity.JobPost;
import com.talent.job.service.IJobApplicationService;
import com.talent.job.service.IJobPostService;
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
    private final IJobPostService jobPostService;

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

    @GetMapping("/application/by-id")
    public Map<String, Object> applicationById(@RequestParam("applicationId") Long applicationId) {
        if (applicationId == null) {
            return Map.of("code", 400, "msg", "applicationId 不能为空");
        }
        JobApplication app = jobApplicationService.getById(applicationId);
        if (app == null) {
            return Map.of("code", 404, "msg", "投递记录不存在");
        }
        Map<String, Object> data = toBrief(app);
        data.put("applicationId", app.getId());
        data.put("jobId", app.getJobId());
        data.put("resumeId", app.getResumeId());
        data.put("candidateId", app.getCandidateId());
        return Map.of("code", 200, "msg", "ok", "data", data);
    }

    @GetMapping("/post/brief")
    public Map<String, Object> postBrief(@RequestParam("jobId") Long jobId) {
        if (jobId == null) {
            return Map.of("code", 400, "msg", "jobId 不能为空");
        }
        JobPost post = jobPostService.getById(jobId);
        if (post == null) {
            return Map.of("code", 404, "msg", "岗位不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", post.getId());
        data.put("title", post.getTitle());
        data.put("workCity", post.getWorkCity());
        data.put("jobDescription", post.getJobDescription());
        data.put("jobRequirements", post.getJobRequirements());
        data.put("skillTags", post.getSkillTags());
        data.put("experienceYearsMin", post.getExperienceYearsMin());
        data.put("educationRequirement", post.getEducationRequirement());
        return Map.of("code", 200, "msg", "ok", "data", data);
    }

    @PostMapping("/application/sync-match-score")
    public Map<String, Object> syncMatchScore(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return Map.of("code", 400, "msg", "请求体不能为空");
        }
        Long applicationId = longVal(body.get("applicationId"));
        Integer matchScore = intVal(body.get("matchScore"));
        if (applicationId == null || matchScore == null) {
            return Map.of("code", 400, "msg", "applicationId 与 matchScore 不能为空");
        }
        JobApplication app = jobApplicationService.getById(applicationId);
        if (app == null) {
            return Map.of("code", 404, "msg", "投递记录不存在");
        }
        int score = Math.max(0, Math.min(100, matchScore));
        app.setMatchScore((byte) score);
        if (!jobApplicationService.updateById(app)) {
            return Map.of("code", 500, "msg", "匹配分更新失败");
        }
        return Map.of("code", 200, "msg", "ok", "data", Map.of("applicationId", applicationId, "matchScore", score));
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
        result.put("applicationId", app.getId());
        result.put("jobId", app.getJobId());
        result.put("resumeId", app.getResumeId());
        result.put("candidateId", app.getCandidateId());
        return result;
    }

    private Long longVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
