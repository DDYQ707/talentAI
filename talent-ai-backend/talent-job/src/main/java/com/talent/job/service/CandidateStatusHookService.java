package com.talent.job.service;

import com.talent.common.api.R;
import com.talent.job.constant.JobApplicationConstants;
import com.talent.job.dto.OfferCreateRequest;
import com.talent.job.entity.JobApplication;
import com.talent.job.feign.AiAgentFeignClient;
import com.talent.job.feign.TalentPoolFeignClient;
import com.talent.job.vo.OfferDetailVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 候选人状态流转联动服务
 * <p>
 * 在投递状态变更（录用 / 淘汰）时执行下游业务逻辑：
 * <ul>
 *   <li>淘汰 → 自动归档至人才库（跨服务 Feign 调用）</li>
 *   <li>录用 → 自动创建 Offer 草稿（本地同库调用）</li>
 *   <li>可选 → 触发 AI 人才画像生成（Feign，失败可忽略）</li>
 * </ul>
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-22
 */
@Service
public class CandidateStatusHookService {

    private static final Logger log = LoggerFactory.getLogger(CandidateStatusHookService.class);

    @Autowired
    private TalentPoolFeignClient talentPoolFeignClient;

    @Autowired
    private AiAgentFeignClient aiAgentFeignClient;

    @Autowired
    private IOfferService offerService;

    // ======================== 淘汰 → 人才库归档 ========================

    /**
     * 候选人被淘汰时，自动将其归档至人才库。
     * <p>
     * 该方法应在本地事务提交后调用，属于跨服务 Feign 调用。
     * 如果人才库归档失败（如候选人已存在），仅打印警告日志，不回滚本地事务。
     * </p>
     *
     * @param app 投递申请实体（状态已更新为 REJECTED）
     */
    public void onRejected(JobApplication app) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("candidateId", app.getCandidateId());
            body.put("candidateName", app.getCandidateName());
            body.put("resumeId", app.getResumeId());
            body.put("sourceApplicationId", app.getId());
            body.put("matchScore", app.getMatchScore());
            body.put("archiveReason", "招聘流程淘汰自动归档");

            Map<String, Object> result = talentPoolFeignClient.archiveToPool(body);

            if (result != null) {
                Object code = result.get("code");
                if (code instanceof Number n && n.longValue() == 200L) {
                    log.info("淘汰归档成功：candidateId={}, applicationId={}", app.getCandidateId(), app.getId());
                } else {
                    // 候选人已在人才库中等情况，静默处理
                    log.warn("淘汰归档未成功：candidateId={}, msg={}",
                            app.getCandidateId(), result.getOrDefault("msg", "未知原因"));
                }
            }
        } catch (Exception e) {
            log.warn("淘汰归档调用人才库服务异常：candidateId={}, error={}", app.getCandidateId(), e.getMessage());
        }
    }

    // ======================== 录用 → 创建 Offer 草稿 ========================

    /**
     * 候选人被录用时，自动创建一条 Offer 草稿记录。
     * <p>
     * 该方法使用本地同库调用 IOfferService.createOffer，参与当前事务。
     * Offer 草稿不设置审批人，按照 OfferServiceImpl 的逻辑将直接进入 "已通过" 状态。
     * 薪资等字段留空，由 HR 后续在 Offer 管理页面补充。
     * </p>
     *
     * @param app        投递申请实体（状态已更新为 HIRED）
     * @param operatorId 操作 HR 的用户 ID（作为 Offer 发起人）
     */
    public void onHired(JobApplication app, Long operatorId) {
        try {
            OfferCreateRequest request = new OfferCreateRequest();
            request.setApplicationId(app.getId());
            request.setJobId(app.getJobId());
            request.setCandidateId(app.getCandidateId());
            request.setCandidateName(app.getCandidateName());
            request.setRemark("录用状态变更自动创建");
            // 不设置审批人，将直接进入"已通过"状态
            request.setApproverIds(null);

            Long hrId = operatorId != null ? operatorId : 0L;
            R<OfferDetailVO> result = offerService.createOffer(hrId, request);

            if (result != null && result.getCode() == 200L) {
                OfferDetailVO offer = result.getData();
                log.info("录用自动创建 Offer 成功：candidateId={}, offerId={}, offerNo={}",
                        app.getCandidateId(),
                        offer != null ? offer.getId() : "N/A",
                        offer != null ? offer.getOfferNo() : "N/A");
            } else {
                log.warn("录用自动创建 Offer 未成功：candidateId={}, msg={}",
                        app.getCandidateId(), result != null ? result.getMsg() : "result is null");
            }
        } catch (Exception e) {
            log.error("录用自动创建 Offer 异常：candidateId={}, error={}", app.getCandidateId(), e.getMessage(), e);
            throw new IllegalStateException("录用联动创建 Offer 失败：" + e.getMessage());
        }
    }

    // ======================== AI 画像触发 ========================

    /**
     * 触发 AI 人才画像生成（可选，失败不影响主流程）。
     * <p>
     * 该方法为 fire-and-forget 模式。如果 AI 服务不可用，
     * 仅打印 debug 日志，不抛出异常、不回滚事务。
     * </p>
     *
     * @param app 投递申请实体
     */
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
            // AI 画像为可选功能，服务不可用时静默忽略
            log.debug("AI 画像触发失败（已忽略）：candidateId={}, error={}", app.getCandidateId(), e.getMessage());
        }
    }
}
