package com.talent.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.talent.job.constant.JobApplicationConstants;
import com.talent.job.dto.JobApplicationSubmitRequest;
import com.talent.job.dto.SyncScreenStatusRequest;
import com.talent.job.entity.ApplicationStageLog;
import com.talent.job.entity.JobApplication;
import com.talent.job.entity.JobPost;
import com.talent.job.feign.AiFeignClient;
import com.talent.job.feign.AuthFeignClient;
import com.talent.job.feign.InterviewFeignClient;
import com.talent.job.feign.ResumeFeignClient;
import lombok.extern.slf4j.Slf4j;
import com.talent.job.mapper.JobApplicationMapper;
import com.talent.job.service.CandidateNotificationService;
import com.talent.job.service.CandidateStatusHookService;
import com.talent.job.service.IApplicationStageLogService;
import com.talent.job.service.IJobApplicationService;
import com.talent.job.service.IJobPostService;
import com.talent.job.vo.JobApplicationListVO;
import com.talent.job.vo.JobApplicationSubmitVO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 投递申请表（talent-job库） 服务实现类
 */
@Slf4j
@Service
public class JobApplicationServiceImpl extends ServiceImpl<JobApplicationMapper, JobApplication> implements IJobApplicationService {

    @Autowired
    private IJobPostService jobPostService;

    @Autowired
    private IApplicationStageLogService applicationStageLogService;

    @Autowired
    private AuthFeignClient authFeignClient;

    @Autowired
    private ResumeFeignClient resumeFeignClient;

    @Autowired
    private AiFeignClient aiFeignClient;

    @Autowired
    private CandidateStatusHookService candidateStatusHookService;

    @Autowired
    private CandidateNotificationService candidateNotificationService;

    @Autowired
    private InterviewFeignClient interviewFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitApplication(Long candidateId, String userRole, JobApplicationSubmitRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (candidateId == null) {
            result.put("code", 401);
            result.put("msg", "未检测到登录用户信息");
            return result;
        }

        if (!JobApplicationConstants.ROLE_CANDIDATE.equals(userRole)) {
            result.put("code", 403);
            result.put("msg", "仅候选人可投递简历");
            return result;
        }

        if (!isCandidateProfileComplete(candidateId)) {
            result.put("code", 400);
            result.put("msg", "请先完善个人信息后再投递简历");
            return result;
        }

        if (request == null || request.getJobId() == null || request.getResumeId() == null) {
            result.put("code", 400);
            result.put("msg", "岗位 ID 与简历 ID 不能为空");
            return result;
        }

        Long resumeId = request.getResumeId();
        Map<String, Object> ownership = resumeFeignClient.getResumeOwnership(resumeId);
        if (ownership == null || !Boolean.TRUE.equals(ownership.get("valid"))) {
            result.put("code", 400);
            result.put("msg", "简历不存在或已失效");
            return result;
        }
        Object ownerId = ownership.get("candidateId");
        if (!(ownerId instanceof Number ownerNum) || ownerNum.longValue() != candidateId) {
            result.put("code", 403);
            result.put("msg", "无权使用该简历投递");
            return result;
        }
        Object primaryResumeId = ownership.get("primaryResumeId");
        if (primaryResumeId instanceof Number primaryNum) {
            resumeId = primaryNum.longValue();
        }

        JobPost jobPost = jobPostService.getById(request.getJobId());
        if (jobPost == null) {
            result.put("code", 404);
            result.put("msg", "岗位不存在");
            return result;
        }
        if (jobPost.getStatus() == null || jobPost.getStatus() != JobApplicationConstants.JOB_STATUS_OPEN) {
            result.put("code", 400);
            result.put("msg", "该岗位当前不可投递");
            return result;
        }

        long activeCount = count(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getJobId, request.getJobId())
                .eq(JobApplication::getCandidateId, candidateId)
                .eq(JobApplication::getStatus, JobApplicationConstants.STATUS_IN_PROGRESS));
        if (activeCount > 0) {
            result.put("code", 409);
            result.put("msg", "您已投递该岗位，请勿重复投递");
            return result;
        }

        String candidateName = authFeignClient.getUserName(candidateId);
        if (!StringUtils.hasText(candidateName)) {
            candidateName = "未知用户";
        }

        LocalDateTime now = LocalDateTime.now();
        Byte channel = request.getChannel() != null ? request.getChannel() : JobApplicationConstants.CHANNEL_OTHER;

        JobApplication application = new JobApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setJobId(request.getJobId());
        application.setJobTitle(jobPost.getTitle());
        application.setCandidateId(candidateId);
        application.setCandidateName(candidateName);
        application.setResumeId(resumeId);
        application.setChannel(channel);
        application.setCurrentStage(JobApplicationConstants.STAGE_RESUME_SUBMITTED);
        application.setStatus(JobApplicationConstants.STATUS_IN_PROGRESS);
        application.setIsStarred(false);
        application.setAppliedAt(now);

        if (!save(application)) {
            result.put("code", 500);
            result.put("msg", "投递记录保存失败");
            return result;
        }

        ApplicationStageLog stageLog = new ApplicationStageLog();
        stageLog.setApplicationId(application.getId());
        stageLog.setFromStage(null);
        stageLog.setToStage(JobApplicationConstants.STAGE_RESUME_SUBMITTED);
        stageLog.setOperatorId(candidateId);
        stageLog.setOperatorName(candidateName);
        stageLog.setActionNote("候选人投递简历");
        if (!applicationStageLogService.save(stageLog)) {
            throw new IllegalStateException("阶段流转日志保存失败");
        }

        boolean updated = jobPostService.update(new LambdaUpdateWrapper<JobPost>()
                .eq(JobPost::getId, request.getJobId())
                .setSql("applied_count = applied_count + 1"));
        if (!updated) {
            throw new IllegalStateException("岗位投递数更新失败");
        }

        markResumePendingOnDelivery(resumeId, candidateId);
        triggerAiParseOnDelivery(application);
        candidateNotificationService.notifyApplicationSubmitted(application);

        JobApplicationSubmitVO vo = new JobApplicationSubmitVO();
        vo.setId(application.getId());
        vo.setApplicationNo(application.getApplicationNo());
        vo.setJobId(application.getJobId());
        vo.setJobTitle(application.getJobTitle());
        vo.setResumeId(application.getResumeId());
        vo.setCurrentStage(application.getCurrentStage());
        vo.setStatus(application.getStatus());
        vo.setAppliedAt(application.getAppliedAt());

        result.put("code", 200);
        result.put("msg", "投递成功");
        result.put("data", vo);
        return result;
    }

    private void markResumePendingOnDelivery(Long resumeId, Long candidateId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("resumeId", resumeId);
            body.put("candidateId", candidateId);
            Map<String, Object> res = resumeFeignClient.markPendingOnDelivery(body);
            if (res == null) {
                return;
            }
            Object code = res.get("code");
            if (code instanceof Number n && n.intValue() != 200) {
                throw new IllegalStateException(String.valueOf(res.getOrDefault("msg", "简历状态更新失败")));
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("简历状态更新失败，请稍后重试");
        }
    }

    /** 投递成功后触发 AI 简历解析；失败不影响主流程 */
    private void triggerAiParseOnDelivery(JobApplication application) {
        if (application == null || application.getResumeId() == null) {
            return;
        }
        try {
            Map<String, Object> ctxRes = resumeFeignClient.getAiParseContext(application.getResumeId());
            if (ctxRes == null) {
                log.warn("投递后触发 AI 解析跳过：上下文为空 applicationId={}", application.getId());
                return;
            }
            Object code = ctxRes.get("code");
            if (!(code instanceof Number codeNum) || codeNum.intValue() != 200) {
                log.warn(
                        "投递后触发 AI 解析跳过：简历上下文查询失败 applicationId={} resumeId={} msg={}",
                        application.getId(),
                        application.getResumeId(),
                        ctxRes.get("msg"));
                return;
            }
            Object data = ctxRes.get("data");
            if (!(data instanceof Map<?, ?> dataMap)) {
                log.warn(
                        "投递后触发 AI 解析跳过：无简历数据 applicationId={} resumeId={}",
                        application.getId(),
                        application.getResumeId());
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> ctx = (Map<String, Object>) dataMap;
            String resumeType = stringVal(ctx.get("resumeType"));
            Long resumeId = longVal(ctx.get("id"));
            if (resumeId == null) {
                resumeId = application.getResumeId();
            }

            Map<String, Object> body = new HashMap<>();
            body.put("resumeId", resumeId);
            body.put("applicationId", application.getId());
            body.put("candidateId", application.getCandidateId());

            if ("attachment".equalsIgnoreCase(resumeType)) {
                Long attachmentId = longVal(ctx.get("attachmentId"));
                if (attachmentId == null) {
                    log.info(
                            "投递后触发 AI 解析跳过：附件信息缺失 applicationId={} resumeId={}",
                            application.getId(),
                            resumeId);
                    return;
                }
                body.put("parseSource", "attachment");
                body.put("attachmentId", attachmentId);
                body.put("fileName", stringVal(ctx.get("fileName")));
                body.put("fileType", stringVal(ctx.get("fileType")));
            } else if ("merged".equalsIgnoreCase(resumeType)) {
                Long attachmentId = longVal(ctx.get("attachmentId"));
                if (attachmentId == null) {
                    log.info(
                            "投递后触发 AI 解析跳过：合并解析缺少附件 applicationId={} resumeId={}",
                            application.getId(),
                            resumeId);
                    return;
                }
                body.put("parseSource", "merged");
                body.put("attachmentId", attachmentId);
                body.put("fileName", stringVal(ctx.get("fileName")));
                body.put("fileType", "merged");
            } else if ("online".equalsIgnoreCase(resumeType)) {
                body.put("parseSource", "online");
            } else {
                log.warn(
                        "投递后触发 AI 解析跳过：未知简历类型 applicationId={} resumeType={}",
                        application.getId(),
                        resumeType);
                return;
            }

            Map<String, Object> res = aiFeignClient.submitParse(body);
            if (res == null) {
                log.warn("投递后触发 AI 解析无响应 applicationId={} resumeId={}", application.getId(), resumeId);
                return;
            }
            Object aiCode = res.get("code");
            if (aiCode instanceof Number aiCodeNum && aiCodeNum.intValue() == 200) {
                Object aiData = res.get("data");
                Long taskId = null;
                if (aiData instanceof Map<?, ?> aiDataMap) {
                    taskId = longVal(aiDataMap.get("taskId"));
                }
                log.info(
                        "投递后已触发 AI 解析 applicationId={} resumeId={} parseSource={} taskId={}",
                        application.getId(),
                        resumeId,
                        body.get("parseSource"),
                        taskId);
                return;
            }
            log.warn(
                    "投递后触发 AI 解析失败 applicationId={} resumeId={} msg={}",
                    application.getId(),
                    resumeId,
                    res.get("msg"));
        } catch (Exception e) {
            log.warn(
                    "投递后触发 AI 解析异常 applicationId={} resumeId={} reason={}",
                    application.getId(),
                    application.getResumeId(),
                    e.getMessage());
        }
    }

    @Override
    public Map<String, Object> listMyApplications(Long candidateId, String userRole, Integer current, Integer size) {
        Map<String, Object> result = new HashMap<>();

        if (candidateId == null) {
            result.put("code", 401);
            result.put("msg", "未检测到登录用户信息");
            return result;
        }

        if (!JobApplicationConstants.ROLE_CANDIDATE.equals(userRole)) {
            result.put("code", 403);
            result.put("msg", "仅候选人可查看投递记录");
            return result;
        }

        int pageNum = current != null && current > 0 ? current : 1;
        int pageSize = size != null && size > 0 ? Math.min(size, 50) : 20;

        Page<JobApplication> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(JobApplication::getCandidateId, candidateId)
                .orderByDesc(JobApplication::getAppliedAt);

        Page<JobApplication> pageResult = page(pageParam, queryWrapper);
        List<JobApplicationListVO> records = pageResult.getRecords().stream()
                .map(this::toListVo)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("total", pageResult.getTotal());
        data.put("current", pageResult.getCurrent());
        data.put("pages", pageResult.getPages());
        data.put("records", records);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> listActiveAppliedJobIds(Long candidateId, String userRole) {
        Map<String, Object> result = new HashMap<>();

        if (candidateId == null) {
            result.put("code", 401);
            result.put("msg", "未检测到登录用户信息");
            return result;
        }

        if (!JobApplicationConstants.ROLE_CANDIDATE.equals(userRole)) {
            result.put("code", 403);
            result.put("msg", "仅候选人可查询投递状态");
            return result;
        }

        List<JobApplication> applications = list(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getCandidateId, candidateId)
                .eq(JobApplication::getStatus, JobApplicationConstants.STATUS_IN_PROGRESS)
                .select(JobApplication::getJobId));

        List<Long> jobIds = applications.stream()
                .map(JobApplication::getJobId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("jobIds", jobIds);

        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncLatestApplicationByScreenStatus(SyncScreenStatusRequest request) {
        Map<String, Object> result = new HashMap<>();
        if (request == null || request.getCandidateId() == null) {
            result.put("code", 400);
            result.put("msg", "candidateId 不能为空");
            return result;
        }
        Integer screenStatus = request.getScreenStatus();
        if (screenStatus == null || screenStatus < 1 || screenStatus > 5) {
            result.put("code", 400);
            result.put("msg", "screenStatus 无效");
            return result;
        }

        JobApplication app = getOne(
                new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getCandidateId, request.getCandidateId())
                        .orderByDesc(JobApplication::getAppliedAt)
                        .last("LIMIT 1"),
                false);
        if (app == null) {
            result.put("code", 200);
            result.put("msg", "无投递记录，已跳过同步");
            result.put("data", Map.of("synced", false));
            return result;
        }

        Byte fromStage = app.getCurrentStage();
        Byte toStage = JobApplicationConstants.stageForScreenStatus(screenStatus);
        Byte newStatus = JobApplicationConstants.applicationStatusForScreenStatus(screenStatus);
        LocalDateTime now = LocalDateTime.now();

        app.setCurrentStage(toStage);
        app.setStatus(newStatus);
        app.setUpdatedAt(now);
        if (newStatus == JobApplicationConstants.STATUS_HIRED) {
            app.setHiredAt(now);
            app.setRejectedAt(null);
        } else if (newStatus == JobApplicationConstants.STATUS_REJECTED) {
            app.setRejectedAt(now);
            app.setHiredAt(null);
        } else {
            app.setHiredAt(null);
            app.setRejectedAt(null);
        }
        if (StringUtils.hasText(request.getRemark())) {
            app.setRemark(request.getRemark().trim());
        }
        if (!updateById(app)) {
            result.put("code", 500);
            result.put("msg", "投递状态更新失败");
            return result;
        }

        String operatorName = StringUtils.hasText(request.getOperatorName())
                ? request.getOperatorName().trim()
                : "HR";
        ApplicationStageLog stageLog = new ApplicationStageLog();
        stageLog.setApplicationId(app.getId());
        stageLog.setFromStage(fromStage);
        stageLog.setToStage(toStage);
        stageLog.setOperatorId(request.getOperatorId());
        stageLog.setOperatorName(operatorName);
        String note = JobApplicationConstants.actionNoteForScreenStatus(screenStatus);
        if (StringUtils.hasText(request.getRemark())) {
            note = note + "：" + request.getRemark().trim();
        }
        stageLog.setActionNote(note);
        if (!applicationStageLogService.save(stageLog)) {
            throw new IllegalStateException("阶段流转日志保存失败");
        }

        // ========== 状态流转联动 ==========
        // 淘汰 + HR 确认归档 → 联动人才库
        if (screenStatus == 4 && Boolean.TRUE.equals(request.getArchiveToTalentPool())) {
            candidateStatusHookService.archiveToTalentPool(app, request);
        }
        // 可选：触发 AI 人才画像生成（fire-and-forget）
        candidateStatusHookService.triggerAiProfile(app);
        candidateNotificationService.notifyScreenStatusChanged(app, screenStatus);

        if (screenStatus == 4) {
            syncInterviewOnRejected(app.getId());
        }

        result.put("code", 200);
        result.put("msg", "同步成功");
        result.put("data", Map.of(
                "synced", true,
                "applicationId", app.getId(),
                "currentStage", toStage,
                "status", newStatus));
        return result;
    }

    private void syncInterviewOnRejected(Long applicationId) {
        if (applicationId == null) {
            return;
        }
        try {
            Map<String, Object> res = interviewFeignClient.syncApplicationRejected(applicationId);
            Object code = res != null ? res.get("code") : null;
            if (code instanceof Number number && number.intValue() != 200) {
                log.warn(
                        "HR 淘汰联动面试服务失败 applicationId={} msg={}",
                        applicationId,
                        res != null ? res.get("msg") : "unknown");
            }
        } catch (Exception e) {
            log.warn("HR 淘汰联动面试服务异常 applicationId={} error={}", applicationId, e.getMessage());
        }
    }

    private JobApplicationListVO toListVo(JobApplication application) {
        JobApplicationListVO vo = new JobApplicationListVO();
        vo.setId(application.getId());
        vo.setApplicationNo(application.getApplicationNo());
        vo.setJobId(application.getJobId());
        vo.setJobTitle(application.getJobTitle());
        vo.setResumeId(application.getResumeId());
        vo.setCurrentStage(application.getCurrentStage());
        vo.setStatus(application.getStatus());
        vo.setMatchScore(application.getMatchScore());
        vo.setAppliedAt(application.getAppliedAt());
        if (application.getResumeId() != null) {
            try {
                Map<String, Object> brief = resumeFeignClient.getResumeBrief(application.getResumeId());
                if (brief != null) {
                    Object data = brief.get("data");
                    if (data instanceof Map<?, ?> dataMap) {
                        vo.setResumeName(stringVal(dataMap.get("resumeName")));
                        vo.setAttachmentId(longVal(dataMap.get("attachmentId")));
                        vo.setAttachmentFileName(stringVal(dataMap.get("fileName")));
                        vo.setAttachmentFileType(stringVal(dataMap.get("fileType")));
                    }
                }
            } catch (Exception ignored) {
                // 简历服务不可用时仍返回投递主信息
            }
        }
        return vo;
    }

    private static String stringVal(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long longVal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isCandidateProfileComplete(Long candidateId) {
        try {
            Map<String, Object> res = authFeignClient.getProfileCompleteness(candidateId);
            if (res == null) {
                return false;
            }
            Object code = res.get("code");
            long codeNum = code instanceof Number n ? n.longValue() : -1L;
            if (codeNum != 200L) {
                return false;
            }
            Object data = res.get("data");
            if (data instanceof Map<?, ?> map) {
                Object complete = map.get("complete");
                return Boolean.TRUE.equals(complete);
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private String generateApplicationNo() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "APP" + timestamp + suffix;
    }
}
