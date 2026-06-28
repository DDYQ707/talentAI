package com.talent.job.service;

import com.talent.job.constant.JobApplicationConstants;
import com.talent.job.entity.ApplicationStageLog;
import com.talent.job.entity.JobApplication;
import com.talent.job.entity.Offer;
import com.talent.job.feign.ResumeFeignClient;
import com.talent.job.mapper.JobApplicationMapper;
import com.talent.job.service.IApplicationStageLogService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Offer 发放/接受/拒绝后的投递状态联动（避免 OfferService 与 JobApplicationService 循环依赖）
 */
@Service
@RequiredArgsConstructor
public class OfferLifecycleHookService {

    private static final Logger log = LoggerFactory.getLogger(OfferLifecycleHookService.class);

    /** resume.screen_status：待录用 */
    private static final int SCREEN_OFFER_PENDING = 5;

    /** resume.screen_status：已录用 */
    private static final int SCREEN_HIRED = 3;

    /** resume.screen_status：已淘汰 */
    private static final int SCREEN_REJECTED = 4;

    private final JobApplicationMapper jobApplicationMapper;
    private final IApplicationStageLogService applicationStageLogService;
    private final CandidateNotificationService candidateNotificationService;
    private final ResumeFeignClient resumeFeignClient;

    public void onOfferIssued(Offer offer) {
        JobApplication app = loadApplication(offer);
        if (app == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Byte fromStage = app.getCurrentStage();
        boolean changed = false;
        if (fromStage == null || fromStage < JobApplicationConstants.STAGE_OFFER) {
            app.setCurrentStage(JobApplicationConstants.STAGE_OFFER);
            changed = true;
        }
        if (app.getStatus() == null || app.getStatus() != JobApplicationConstants.STATUS_IN_PROGRESS) {
            app.setStatus(JobApplicationConstants.STATUS_IN_PROGRESS);
            app.setHiredAt(null);
            app.setRejectedAt(null);
            changed = true;
        }
        if (changed) {
            app.setUpdatedAt(now);
            jobApplicationMapper.updateById(app);
            saveStageLog(app.getId(), fromStage, JobApplicationConstants.STAGE_OFFER, null, "系统", "Offer 已发放");
        }
        syncResumeScreenStatus(app, SCREEN_OFFER_PENDING, "Offer 已发放，待候选人确认");
        candidateNotificationService.notifyOfferIssued(app, offer);
    }

    public void onOfferAccepted(Offer offer, Long candidateId) {
        JobApplication app = loadApplication(offer);
        if (app == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Byte fromStage = app.getCurrentStage();
        app.setCurrentStage(JobApplicationConstants.STAGE_ONBOARDED);
        app.setStatus(JobApplicationConstants.STATUS_HIRED);
        app.setHiredAt(now);
        app.setRejectedAt(null);
        app.setUpdatedAt(now);
        jobApplicationMapper.updateById(app);
        saveStageLog(
                app.getId(),
                fromStage,
                JobApplicationConstants.STAGE_ONBOARDED,
                candidateId,
                app.getCandidateName(),
                "候选人接受 Offer");
        syncResumeScreenStatus(app, SCREEN_HIRED, "候选人接受 Offer");
        candidateNotificationService.notifyOfferAccepted(app, offer);
    }

    public void onOfferDeclined(Offer offer, Long candidateId) {
        JobApplication app = loadApplication(offer);
        if (app == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Byte fromStage = app.getCurrentStage();
        app.setStatus(JobApplicationConstants.STATUS_REJECTED);
        app.setRejectedAt(now);
        app.setUpdatedAt(now);
        jobApplicationMapper.updateById(app);
        saveStageLog(
                app.getId(),
                fromStage,
                app.getCurrentStage(),
                candidateId,
                app.getCandidateName(),
                "候选人拒绝 Offer");
        syncResumeScreenStatus(app, SCREEN_REJECTED, "候选人拒绝 Offer");
        candidateNotificationService.notifyOfferDeclined(app, offer);
    }

    private void syncResumeScreenStatus(JobApplication app, int screenStatus, String remark) {
        if (app == null || app.getCandidateId() == null) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("candidateId", app.getCandidateId());
            body.put("resumeId", app.getResumeId());
            body.put("screenStatus", screenStatus);
            body.put("remark", remark);
            Map<String, Object> res = resumeFeignClient.setScreenStatusOnly(body);
            if (res == null) {
                log.warn("同步简历筛选状态无响应 candidateId={} screenStatus={}", app.getCandidateId(), screenStatus);
                return;
            }
            Object code = res.get("code");
            if (!(code instanceof Number num) || num.intValue() != 200) {
                log.warn(
                        "同步简历筛选状态失败 candidateId={} screenStatus={} msg={}",
                        app.getCandidateId(),
                        screenStatus,
                        res.get("msg"));
            }
        } catch (Exception e) {
            log.warn(
                    "同步简历筛选状态异常 candidateId={} screenStatus={} error={}",
                    app.getCandidateId(),
                    screenStatus,
                    e.getMessage());
        }
    }

    private JobApplication loadApplication(Offer offer) {
        if (offer == null || offer.getApplicationId() == null) {
            return null;
        }
        return jobApplicationMapper.selectById(offer.getApplicationId());
    }

    private void saveStageLog(
            Long applicationId,
            Byte fromStage,
            Byte toStage,
            Long operatorId,
            String operatorName,
            String actionNote) {
        ApplicationStageLog stageLog = new ApplicationStageLog();
        stageLog.setApplicationId(applicationId);
        stageLog.setFromStage(fromStage);
        stageLog.setToStage(toStage);
        stageLog.setOperatorId(operatorId);
        stageLog.setOperatorName(operatorName != null ? operatorName : "系统");
        stageLog.setActionNote(actionNote);
        applicationStageLogService.save(stageLog);
    }
}
