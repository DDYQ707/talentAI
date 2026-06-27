package com.talent.job.service;

import com.talent.job.constant.JobApplicationConstants;
import com.talent.job.entity.ApplicationStageLog;
import com.talent.job.entity.JobApplication;
import com.talent.job.entity.Offer;
import com.talent.job.mapper.JobApplicationMapper;
import com.talent.job.service.IApplicationStageLogService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Offer 发放/接受/拒绝后的投递状态联动（避免 OfferService 与 JobApplicationService 循环依赖）
 */
@Service
@RequiredArgsConstructor
public class OfferLifecycleHookService {

    private final JobApplicationMapper jobApplicationMapper;
    private final IApplicationStageLogService applicationStageLogService;
    private final CandidateNotificationService candidateNotificationService;

    public void onOfferIssued(Offer offer) {
        JobApplication app = loadApplication(offer);
        if (app == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Byte fromStage = app.getCurrentStage();
        Byte fromStatus = app.getStatus();
        boolean changed = false;
        if (fromStage == null || fromStage < JobApplicationConstants.STAGE_OFFER) {
            app.setCurrentStage(JobApplicationConstants.STAGE_OFFER);
            changed = true;
        }
        if (fromStatus == null || fromStatus != JobApplicationConstants.STATUS_HIRED) {
            app.setStatus(JobApplicationConstants.STATUS_HIRED);
            app.setHiredAt(now);
            app.setRejectedAt(null);
            changed = true;
        }
        if (changed) {
            app.setUpdatedAt(now);
            jobApplicationMapper.updateById(app);
            saveStageLog(app.getId(), fromStage, JobApplicationConstants.STAGE_OFFER, null, "系统", "Offer 已发放");
        }
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
        app.setUpdatedAt(now);
        jobApplicationMapper.updateById(app);
        saveStageLog(
                app.getId(),
                fromStage,
                JobApplicationConstants.STAGE_ONBOARDED,
                candidateId,
                app.getCandidateName(),
                "候选人接受 Offer");
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
        candidateNotificationService.notifyOfferDeclined(app, offer);
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
