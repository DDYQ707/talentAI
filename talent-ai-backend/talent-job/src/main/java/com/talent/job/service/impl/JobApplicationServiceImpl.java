package com.talent.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.talent.job.constant.JobApplicationConstants;
import com.talent.job.dto.JobApplicationSubmitRequest;
import com.talent.job.entity.ApplicationStageLog;
import com.talent.job.entity.JobApplication;
import com.talent.job.entity.JobPost;
import com.talent.job.feign.AuthFeignClient;
import com.talent.job.feign.ResumeFeignClient;
import com.talent.job.mapper.JobApplicationMapper;
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

        if (request == null || request.getJobId() == null || request.getResumeId() == null) {
            result.put("code", 400);
            result.put("msg", "岗位 ID 与简历 ID 不能为空");
            return result;
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
        application.setResumeId(request.getResumeId());
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

    private String generateApplicationNo() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "APP" + timestamp + suffix;
    }
}
