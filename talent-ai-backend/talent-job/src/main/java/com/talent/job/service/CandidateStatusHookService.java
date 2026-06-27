package com.talent.job.service;

import com.talent.common.api.R;
import com.talent.job.dto.OfferCreateRequest;
import com.talent.job.entity.JobApplication;
import com.talent.job.feign.AiAgentFeignClient;
import com.talent.job.vo.OfferDetailVO;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 候选人状态流转联动服务
 */
@Service
public class CandidateStatusHookService {

    private static final Logger log = LoggerFactory.getLogger(CandidateStatusHookService.class);

    @Autowired
    private AiAgentFeignClient aiAgentFeignClient;

    @Autowired
    private IOfferService offerService;

    public void onHired(JobApplication app, Long operatorId) {
        try {
            OfferCreateRequest request = new OfferCreateRequest();
            request.setApplicationId(app.getId());
            request.setJobId(app.getJobId());
            request.setCandidateId(app.getCandidateId());
            request.setCandidateName(app.getCandidateName());
            request.setRemark("录用状态变更自动创建");
            request.setApproverIds(null);

            Long hrId = operatorId != null ? operatorId : 0L;
            R<OfferDetailVO> result = offerService.createOffer(hrId, request);

            if (result != null && result.getCode() == 200L) {
                OfferDetailVO offer = result.getData();
                log.info(
                        "录用自动创建 Offer 成功：candidateId={}, offerId={}, offerNo={}",
                        app.getCandidateId(),
                        offer != null ? offer.getId() : "N/A",
                        offer != null ? offer.getOfferNo() : "N/A");
            } else {
                log.warn(
                        "录用自动创建 Offer 未成功：candidateId={}, msg={}",
                        app.getCandidateId(),
                        result != null ? result.getMsg() : "result is null");
            }
        } catch (Exception e) {
            log.error(
                    "录用自动创建 Offer 异常（已忽略，录用状态仍生效）：candidateId={}, error={}",
                    app.getCandidateId(),
                    e.getMessage(),
                    e);
        }
    }

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
}
