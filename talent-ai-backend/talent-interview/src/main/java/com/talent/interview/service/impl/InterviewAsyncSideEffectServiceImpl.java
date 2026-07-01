package com.talent.interview.service.impl;

import com.talent.interview.constant.InterviewConstants;
import com.talent.interview.entity.Interview;
import com.talent.interview.feign.AiAgentFeignClient;
import com.talent.interview.feign.JobFeignClient;
import com.talent.interview.feign.ResumeFeignClient;
import com.talent.interview.service.InterviewAsyncSideEffectService;
import com.talent.interview.util.FeignResponseHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAsyncSideEffectServiceImpl implements InterviewAsyncSideEffectService {

    private static final int SCREEN_STATUS_PENDING = 1;
    private static final int SCREEN_STATUS_OFFER_PENDING = 5;

    private final JobFeignClient jobFeignClient;
    private final ResumeFeignClient resumeFeignClient;
    private final AiAgentFeignClient aiAgentFeignClient;

    @Override
    @Async
    public void afterEvaluationSubmitted(Interview interview, Integer conclusion) {
        if (interview == null) {
            return;
        }
        if (Objects.equals(conclusion, InterviewConstants.CONCLUSION_PASS)) {
            syncOfferPendingScreenStatus(interview);
        }
        triggerAiProfileAfterEvaluation(interview);
    }

    @Override
    @Async
    public void revertToPendingScreenAfterCancel(Interview interview) {
        if (interview == null) {
            return;
        }
        Long candidateId = interview.getCandidateId();
        if (candidateId == null) {
            return;
        }
        Long resumeId = resolveResumeId(interview);
        try {
            Map<String, Object> resumeBody = new HashMap<>();
            resumeBody.put("resumeId", resumeId);
            resumeBody.put("candidateId", candidateId);
            resumeBody.put("screenStatus", SCREEN_STATUS_PENDING);
            resumeBody.put("operatorId", interview.getCreatedBy());
            resumeBody.put("operatorName", interview.getCreatedByName());
            resumeBody.put("remark", "面试取消回退待筛选");
            Map<String, Object> resumeRes = resumeFeignClient.syncScreenStatus(resumeBody);
            if (FeignResponseHelper.code(resumeRes) != 200) {
                log.warn(
                        "取消面试后回退筛选状态失败 candidateId={} msg={}",
                        candidateId,
                        FeignResponseHelper.msg(resumeRes, ""));
            }

            Map<String, Object> jobBody = new HashMap<>();
            jobBody.put("candidateId", candidateId);
            jobBody.put("screenStatus", SCREEN_STATUS_PENDING);
            jobBody.put("operatorId", interview.getCreatedBy());
            jobBody.put("operatorName", interview.getCreatedByName());
            jobBody.put("remark", "面试取消回退待筛选");
            Map<String, Object> jobRes = jobFeignClient.syncByScreenStatus(jobBody);
            if (FeignResponseHelper.code(jobRes) != 200) {
                log.warn(
                        "取消面试后同步投递阶段失败 candidateId={} msg={}",
                        candidateId,
                        FeignResponseHelper.msg(jobRes, ""));
            }
        } catch (Exception e) {
            log.warn("取消面试后回退筛选状态异常 candidateId={}", candidateId, e);
        }
    }

    private void syncOfferPendingScreenStatus(Interview interview) {
        if (interview.getApplicationId() == null) {
            return;
        }
        try {
            Map<String, Object> appRes = jobFeignClient.applicationById(interview.getApplicationId());
            if (FeignResponseHelper.code(appRes) != 200) {
                log.warn(
                        "面试通过后同步待录用失败：投递单查询失败 applicationId={} msg={}",
                        interview.getApplicationId(),
                        FeignResponseHelper.msg(appRes, ""));
                return;
            }
            Map<String, Object> appData = FeignResponseHelper.dataMap(appRes);
            Long candidateId = FeignResponseHelper.longVal(appData.get("candidateId"));
            if (candidateId == null) {
                return;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("candidateId", candidateId);
            body.put("resumeId", FeignResponseHelper.longVal(appData.get("resumeId")));
            body.put("screenStatus", SCREEN_STATUS_OFFER_PENDING);
            body.put("remark", "面试官评价通过");
            Map<String, Object> res = resumeFeignClient.setScreenStatusOnly(body);
            if (FeignResponseHelper.code(res) != 200) {
                log.warn(
                        "面试通过后更新待录用状态失败 applicationId={} msg={}",
                        interview.getApplicationId(),
                        FeignResponseHelper.msg(res, ""));
            }
        } catch (Exception e) {
            log.warn(
                    "面试通过后更新待录用状态异常 applicationId={} error={}",
                    interview.getApplicationId(),
                    e.getMessage());
        }
    }

    private void triggerAiProfileAfterEvaluation(Interview interview) {
        if (interview.getApplicationId() == null) {
            return;
        }
        try {
            Map<String, Object> appRes = jobFeignClient.applicationById(interview.getApplicationId());
            if (FeignResponseHelper.code(appRes) != 200) {
                log.warn(
                        "面试评价后触发 AI 画像失败：投递单查询失败 applicationId={} msg={}",
                        interview.getApplicationId(),
                        FeignResponseHelper.msg(appRes, ""));
                return;
            }
            Map<String, Object> appData = FeignResponseHelper.dataMap(appRes);
            Map<String, Object> body = new HashMap<>();
            body.put("candidateId", FeignResponseHelper.longVal(appData.get("candidateId")));
            body.put("candidateName", FeignResponseHelper.strVal(appData.get("candidateName")));
            body.put("resumeId", FeignResponseHelper.longVal(appData.get("resumeId")));
            body.put("applicationId", interview.getApplicationId());
            body.put("jobId", FeignResponseHelper.longVal(appData.get("jobId")));
            body.put("jobTitle", FeignResponseHelper.strVal(appData.get("jobTitle")));
            body.put("status", FeignResponseHelper.intVal(appData.get("status")));

            Map<String, Object> result = aiAgentFeignClient.generateProfile(body);
            log.info(
                    "面试评价后 AI 画像触发完成 applicationId={} result={}",
                    interview.getApplicationId(),
                    result);
        } catch (Exception e) {
            log.warn(
                    "面试评价后 AI 画像触发失败（已忽略） applicationId={} error={}",
                    interview.getApplicationId(),
                    e.getMessage());
        }
    }

    private Long resolveResumeId(Interview interview) {
        Long applicationId = interview.getApplicationId();
        Long candidateId = interview.getCandidateId();

        if (applicationId != null) {
            try {
                Map<String, Object> appRes = jobFeignClient.applicationById(applicationId);
                if (FeignResponseHelper.code(appRes) == 200) {
                    Long resumeId = FeignResponseHelper.longVal(FeignResponseHelper.dataMap(appRes).get("resumeId"));
                    if (resumeId != null) {
                        return resumeId;
                    }
                }
            } catch (Exception e) {
                log.warn("查询投递 resumeId 失败 applicationId={}", applicationId, e);
            }
        }
        if (candidateId != null) {
            try {
                Map<String, Object> resumeRes = resumeFeignClient.primaryByCandidate(candidateId);
                if (FeignResponseHelper.code(resumeRes) == 200) {
                    return FeignResponseHelper.longVal(FeignResponseHelper.dataMap(resumeRes).get("resumeId"));
                }
            } catch (Exception e) {
                log.warn("查询候选人主简历失败 candidateId={}", candidateId, e);
            }
        }
        return null;
    }
}
