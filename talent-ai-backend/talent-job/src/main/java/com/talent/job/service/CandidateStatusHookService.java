package com.talent.job.service;

import com.talent.job.dto.SyncScreenStatusRequest;
import com.talent.job.entity.JobApplication;
import com.talent.job.feign.AiAgentFeignClient;
import com.talent.job.feign.TalentPoolFeignClient;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 候选人状态流转联动服务
 */
@Service
public class CandidateStatusHookService {

    private static final Logger log = LoggerFactory.getLogger(CandidateStatusHookService.class);

    @Autowired
    private AiAgentFeignClient aiAgentFeignClient;

    @Autowired
    private TalentPoolFeignClient talentPoolFeignClient;

    public void triggerAiProfile(JobApplication app) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("candidateId", app.getCandidateId());
            body.put("candidateName", app.getCandidateName());
            body.put("resumeId", app.getResumeId());
            body.put("applicationId", app.getId());
            body.put("jobId", app.getJobId());
            body.put("jobTitle", app.getJobTitle());
            body.put("status", app.getStatus());

            Map<String, Object> result = aiAgentFeignClient.generateProfile(body);
            if (result != null) {
                log.info("AI 画像触发完成：candidateId={}, result={}", app.getCandidateId(), result);
            }
        } catch (Exception e) {
            log.debug("AI 画像触发失败（已忽略）：candidateId={}, error={}", app.getCandidateId(), e.getMessage());
        }
    }

    /**
     * 淘汰等场景：HR 确认后将候选人归档至人才库
     */
    public void archiveToTalentPool(JobApplication app, SyncScreenStatusRequest request) {
        if (app == null || app.getCandidateId() == null) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("candidateId", app.getCandidateId());
            body.put("candidateName", app.getCandidateName());
            body.put("resumeId", app.getResumeId());
            body.put("sourceApplicationId", app.getId());
            if (StringUtils.hasText(app.getJobTitle())) {
                body.put("sourceJobTitle", app.getJobTitle().trim());
            }
            if (request != null && StringUtils.hasText(request.getArchiveReason())) {
                body.put("archiveReason", request.getArchiveReason().trim());
            } else {
                body.put("archiveReason", "淘汰后归档");
            }
            if (request != null && StringUtils.hasText(request.getInterviewSummary())) {
                body.put("interviewSummary", request.getInterviewSummary().trim());
            }
            Map<String, Object> result = talentPoolFeignClient.archive(body);
            Object code = result != null ? result.get("code") : null;
            if (code instanceof Number n && n.longValue() == 200L) {
                log.info("淘汰联动归档人才库成功：candidateId={}", app.getCandidateId());
            } else {
                log.warn(
                        "淘汰联动归档人才库未成功：candidateId={}, msg={}",
                        app.getCandidateId(),
                        result != null ? result.get("msg") : "result is null");
            }
        } catch (Exception e) {
            log.warn(
                    "淘汰联动归档人才库异常（已忽略，淘汰状态仍生效）：candidateId={}, error={}",
                    app.getCandidateId(),
                    e.getMessage());
        }
    }
}
