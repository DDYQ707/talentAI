package com.talent.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.talent.interview.constant.InterviewConstants;
import com.talent.interview.dto.InterviewScheduleRequest;
import com.talent.interview.entity.Interview;
import com.talent.interview.feign.AuthFeignClient;
import com.talent.interview.feign.JobFeignClient;
import com.talent.interview.feign.ResumeFeignClient;
import com.talent.interview.mapper.InterviewMapper;
import com.talent.interview.service.InterviewEvaluationService;
import com.talent.interview.service.InterviewService;
import com.talent.interview.util.FeignResponseHelper;
import com.talent.interview.util.InterviewAuthSupport;
import com.talent.interview.vo.InterviewBriefVO;
import com.talent.interview.vo.InterviewDetailVO;
import com.talent.interview.vo.InterviewEvaluationVO;
import com.talent.interview.vo.InterviewListVO;
import com.talent.interview.vo.InterviewScheduleResultVO;
import com.talent.interview.vo.InterviewStatsVO;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private static final byte APPLICATION_IN_PROGRESS = 1;
    private static final byte USER_TYPE_INTERVIEWER = 3;
    private static final int SCREEN_STATUS_INTERVIEWING = 2;

    private final InterviewMapper interviewMapper;
    private final InterviewEvaluationService evaluationService;
    private final JobFeignClient jobFeignClient;
    private final AuthFeignClient authFeignClient;
    private final ResumeFeignClient resumeFeignClient;

    @Override
    public Interview getById(Long interviewId) {
        if (interviewId == null) {
            return null;
        }
        return interviewMapper.selectById(interviewId);
    }

    @Override
    public List<Interview> listByApplicationId(Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }
        return interviewMapper.selectList(
                new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getApplicationId, applicationId)
                        .orderByAsc(Interview::getScheduledStart));
    }

    @Override
    public InterviewBriefVO toBrief(Interview interview) {
        if (interview == null) {
            return null;
        }
        InterviewBriefVO vo = new InterviewBriefVO();
        vo.setInterviewId(interview.getId());
        vo.setApplicationId(interview.getApplicationId());
        vo.setJobId(interview.getJobId());
        vo.setCandidateId(interview.getCandidateId());
        vo.setCandidateName(interview.getCandidateName());
        vo.setJobTitle(interview.getJobTitle());
        vo.setInterviewerId(interview.getInterviewerId());
        vo.setInterviewerName(interview.getInterviewerName());
        vo.setRoundNo(interview.getRoundNo());
        vo.setRoundType(interview.getRoundType());
        vo.setStatus(interview.getStatus());
        vo.setScheduledStart(interview.getScheduledStart());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewScheduleResultVO schedule(
            String role, Long hrId, String hrName, InterviewScheduleRequest request) {
        InterviewAuthSupport.requireHr(role);
        InterviewAuthSupport.requireUserId(hrId);
        validateScheduleRequest(request);

        String resolvedHrName = resolveDisplayName(hrId, hrName);

        Map<String, Object> appData = loadApplication(request.getApplicationId());
        Integer appStatus = FeignResponseHelper.intVal(appData.get("status"));
        if (appStatus == null || appStatus != APPLICATION_IN_PROGRESS) {
            throw new IllegalArgumentException("投递已结束或已淘汰，无法安排面试");
        }

        Map<String, Object> interviewer = loadUserBrief(request.getInterviewerId());
        Integer userType = FeignResponseHelper.intVal(interviewer.get("userType"));
        if (userType == null || userType != USER_TYPE_INTERVIEWER) {
            throw new IllegalArgumentException("所选用户不是面试官账号");
        }

        int roundNo = request.getRoundNo() != null && request.getRoundNo() > 0 ? request.getRoundNo() : 1;
        long duplicate = interviewMapper.selectCount(
                new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getApplicationId, request.getApplicationId())
                        .eq(Interview::getRoundNo, roundNo)
                        .ne(Interview::getStatus, InterviewConstants.STATUS_CANCELLED));
        if (duplicate > 0) {
            throw new IllegalArgumentException("该投递的第 " + roundNo + " 轮面试已存在，请勿重复安排");
        }

        Interview interview = new Interview();
        interview.setApplicationId(request.getApplicationId());
        interview.setJobId(FeignResponseHelper.longVal(appData.get("jobId")));
        interview.setCandidateId(FeignResponseHelper.longVal(appData.get("candidateId")));
        interview.setCandidateName(defaultName(FeignResponseHelper.strVal(appData.get("candidateName")), "候选人"));
        interview.setJobTitle(defaultName(FeignResponseHelper.strVal(appData.get("jobTitle")), "岗位"));
        interview.setInterviewerId(request.getInterviewerId());
        interview.setInterviewerName(defaultName(FeignResponseHelper.strVal(interviewer.get("nickname")), "面试官"));
        interview.setRoundNo(roundNo);
        interview.setRoundType(request.getRoundType());
        interview.setInterviewMode(request.getInterviewMode());
        interview.setScheduledStart(request.getScheduledStart());
        interview.setScheduledEnd(request.getScheduledEnd());
        interview.setMeetingUrl(trimToNull(request.getMeetingUrl()));
        interview.setLocation(trimToNull(request.getLocation()));
        interview.setStatus(InterviewConstants.STATUS_PENDING);
        interview.setCreatedBy(hrId);
        interview.setCreatedByName(resolvedHrName);
        interview.setCreatedAt(LocalDateTime.now());
        interview.setUpdatedAt(LocalDateTime.now());
        interviewMapper.insert(interview);

        syncInterviewStage(appData, hrId, resolvedHrName);

        InterviewScheduleResultVO result = new InterviewScheduleResultVO();
        result.setInterviewId(interview.getId());
        result.setStatus(interview.getStatus());
        result.setStatusLabel(InterviewConstants.statusLabel(interview.getStatus()));
        result.setCandidateName(interview.getCandidateName());
        result.setJobTitle(interview.getJobTitle());
        result.setInterviewerName(interview.getInterviewerName());
        result.setScheduledStart(interview.getScheduledStart());
        return result;
    }

    @Override
    public Map<String, Object> pageForHr(
            String role, Integer page, Integer size, String keyword, Integer status,
            LocalDate dateFrom, LocalDate dateTo) {
        InterviewAuthSupport.requireHr(role);
        long current = page != null && page > 0 ? page : 1;
        long pageSize = size != null && size > 0 ? Math.min(size, 100) : 10;

        LambdaQueryWrapper<Interview> wrapper = buildListWrapper(keyword, status, dateFrom, dateTo, null);
        wrapper.orderByDesc(Interview::getScheduledStart);

        Page<Interview> pageResult = interviewMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<InterviewListVO> records = pageResult.getRecords().stream()
                .map(this::toListVo)
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", pageResult.getTotal());
        data.put("page", pageResult.getCurrent());
        data.put("size", pageResult.getSize());
        return data;
    }

    @Override
    public InterviewStatsVO statsForHr(String role) {
        InterviewAuthSupport.requireHr(role);
        return buildStats(null);
    }

    @Override
    public InterviewDetailVO detailForHr(String role, Long interviewId) {
        InterviewAuthSupport.requireHr(role);
        Interview interview = requireInterview(interviewId);
        return toDetailVo(interview);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelForHr(String role, Long interviewId) {
        InterviewAuthSupport.requireHr(role);
        Interview interview = requireInterview(interviewId);
        if (interview.getStatus() == null || interview.getStatus() != InterviewConstants.STATUS_PENDING) {
            throw new IllegalArgumentException("仅待进行的面试可取消");
        }
        interview.setStatus(InterviewConstants.STATUS_CANCELLED);
        interview.setUpdatedAt(LocalDateTime.now());
        interviewMapper.updateById(interview);
    }

    @Override
    public List<InterviewListVO> listByApplicationForHr(String role, Long applicationId) {
        InterviewAuthSupport.requireHr(role);
        if (applicationId == null) {
            throw new IllegalArgumentException("applicationId 不能为空");
        }
        return listByApplicationId(applicationId).stream()
                .map(this::toListVo)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> pageForInterviewer(
            String role, Long interviewerId, Integer page, Integer size, String keyword, Integer status) {
        InterviewAuthSupport.requireInterviewer(role);
        InterviewAuthSupport.requireUserId(interviewerId);

        long current = page != null && page > 0 ? page : 1;
        long pageSize = size != null && size > 0 ? Math.min(size, 100) : 10;

        LambdaQueryWrapper<Interview> wrapper = buildListWrapper(keyword, status, null, null, interviewerId);
        wrapper.orderByAsc(Interview::getScheduledStart);

        Page<Interview> pageResult = interviewMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<InterviewListVO> records = pageResult.getRecords().stream()
                .map(this::toListVo)
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", pageResult.getTotal());
        data.put("page", pageResult.getCurrent());
        data.put("size", pageResult.getSize());
        return data;
    }

    @Override
    public InterviewStatsVO statsForInterviewer(String role, Long interviewerId) {
        InterviewAuthSupport.requireInterviewer(role);
        InterviewAuthSupport.requireUserId(interviewerId);
        return buildStats(interviewerId);
    }

    @Override
    public InterviewDetailVO detailForInterviewer(String role, Long interviewerId, Long interviewId) {
        InterviewAuthSupport.requireInterviewer(role);
        InterviewAuthSupport.requireUserId(interviewerId);
        Interview interview = requireInterview(interviewId);
        if (!interviewerId.equals(interview.getInterviewerId())) {
            throw new IllegalArgumentException("无权查看该面试");
        }
        return toDetailVo(interview);
    }

    private void validateScheduleRequest(InterviewScheduleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getApplicationId() == null) {
            throw new IllegalArgumentException("applicationId 不能为空");
        }
        if (request.getInterviewerId() == null) {
            throw new IllegalArgumentException("interviewerId 不能为空");
        }
        if (!InterviewConstants.isValidRoundType(request.getRoundType())) {
            throw new IllegalArgumentException("roundType 无效，应为 1～7");
        }
        if (!InterviewConstants.isValidInterviewMode(request.getInterviewMode())) {
            throw new IllegalArgumentException("interviewMode 无效，应为 1～3");
        }
        if (request.getScheduledStart() == null) {
            throw new IllegalArgumentException("scheduledStart 不能为空");
        }
        if (request.getInterviewMode() == 1 && !StringUtils.hasText(request.getMeetingUrl())) {
            throw new IllegalArgumentException("视频面试请填写 meetingUrl");
        }
        if (request.getInterviewMode() == 2 && !StringUtils.hasText(request.getLocation())) {
            throw new IllegalArgumentException("现场面试请填写 location");
        }
    }

    private Map<String, Object> loadApplication(Long applicationId) {
        Map<String, Object> response = jobFeignClient.applicationById(applicationId);
        if (FeignResponseHelper.code(response) != 200) {
            throw new IllegalArgumentException(FeignResponseHelper.msg(response, "投递记录不存在"));
        }
        Map<String, Object> data = FeignResponseHelper.dataMap(response);
        if (data.isEmpty()) {
            throw new IllegalArgumentException("投递记录不存在");
        }
        return data;
    }

    private Map<String, Object> loadUserBrief(Long userId) {
        Map<String, Object> response = authFeignClient.userBrief(userId);
        if (FeignResponseHelper.code(response) != 200) {
            throw new IllegalArgumentException(FeignResponseHelper.msg(response, "用户不存在"));
        }
        Map<String, Object> data = FeignResponseHelper.dataMap(response);
        if (data.isEmpty()) {
            throw new IllegalArgumentException("用户不存在");
        }
        return data;
    }

    private void syncInterviewStage(Map<String, Object> appData, Long operatorId, String operatorName) {
        Long candidateId = FeignResponseHelper.longVal(appData.get("candidateId"));
        Long resumeId = FeignResponseHelper.longVal(appData.get("resumeId"));
        if (candidateId == null) {
            return;
        }
        try {
            Map<String, Object> resumeBody = new HashMap<>();
            resumeBody.put("resumeId", resumeId);
            resumeBody.put("candidateId", candidateId);
            resumeBody.put("screenStatus", SCREEN_STATUS_INTERVIEWING);
            resumeBody.put("operatorId", operatorId);
            resumeBody.put("operatorName", operatorName);
            resumeBody.put("remark", "安排面试");
            Map<String, Object> resumeRes = resumeFeignClient.syncScreenStatus(resumeBody);
            if (FeignResponseHelper.code(resumeRes) != 200) {
                log.warn("同步简历筛选状态失败 candidateId={} msg={}", candidateId, FeignResponseHelper.msg(resumeRes, ""));
            }

            Map<String, Object> jobBody = new HashMap<>();
            jobBody.put("candidateId", candidateId);
            jobBody.put("screenStatus", SCREEN_STATUS_INTERVIEWING);
            jobBody.put("operatorId", operatorId);
            jobBody.put("operatorName", operatorName);
            jobBody.put("remark", "安排面试");
            Map<String, Object> jobRes = jobFeignClient.syncByScreenStatus(jobBody);
            if (FeignResponseHelper.code(jobRes) != 200) {
                log.warn("同步投递阶段失败 candidateId={} msg={}", candidateId, FeignResponseHelper.msg(jobRes, ""));
            }
        } catch (Exception e) {
            log.warn("安排面试后同步阶段异常 candidateId={}", candidateId, e);
        }
    }

    private LambdaQueryWrapper<Interview> buildListWrapper(
            String keyword, Integer status, LocalDate dateFrom, LocalDate dateTo, Long interviewerId) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        if (interviewerId != null) {
            wrapper.eq(Interview::getInterviewerId, interviewerId);
        }
        if (status != null && InterviewConstants.isValidStatus(status)) {
            wrapper.eq(Interview::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Interview::getCandidateName, kw).or().like(Interview::getJobTitle, kw));
        }
        if (dateFrom != null) {
            wrapper.ge(Interview::getScheduledStart, dateFrom.atStartOfDay());
        }
        if (dateTo != null) {
            wrapper.lt(Interview::getScheduledStart, dateTo.plusDays(1).atStartOfDay());
        }
        return wrapper;
    }

    private InterviewStatsVO buildStats(Long interviewerId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        LocalDateTime weekStart = weekStartDate.atStartOfDay();
        LocalDateTime weekEnd = weekStartDate.plusDays(7).atStartOfDay();

        InterviewStatsVO stats = new InterviewStatsVO();
        stats.setTodayPending(countBy(interviewerId, InterviewConstants.STATUS_PENDING, todayStart, todayEnd));
        stats.setWeekTotal(countScheduledBetween(interviewerId, weekStart, weekEnd));
        stats.setCompleted(countByStatus(interviewerId, InterviewConstants.STATUS_COMPLETED));
        stats.setToSchedule(countByStatus(interviewerId, InterviewConstants.STATUS_TO_SCHEDULE));
        stats.setCancelled(countByStatus(interviewerId, InterviewConstants.STATUS_CANCELLED));
        return stats;
    }

    private long countBy(Long interviewerId, int status, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<Interview>()
                .eq(Interview::getStatus, status)
                .ge(Interview::getScheduledStart, from)
                .lt(Interview::getScheduledStart, to);
        if (interviewerId != null) {
            wrapper.eq(Interview::getInterviewerId, interviewerId);
        }
        return interviewMapper.selectCount(wrapper);
    }

    private long countScheduledBetween(Long interviewerId, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<Interview>()
                .ge(Interview::getScheduledStart, from)
                .lt(Interview::getScheduledStart, to)
                .ne(Interview::getStatus, InterviewConstants.STATUS_CANCELLED);
        if (interviewerId != null) {
            wrapper.eq(Interview::getInterviewerId, interviewerId);
        }
        return interviewMapper.selectCount(wrapper);
    }

    private long countByStatus(Long interviewerId, int status) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<Interview>()
                .eq(Interview::getStatus, status);
        if (interviewerId != null) {
            wrapper.eq(Interview::getInterviewerId, interviewerId);
        }
        return interviewMapper.selectCount(wrapper);
    }

    private Interview requireInterview(Long interviewId) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 不能为空");
        }
        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new IllegalArgumentException("面试记录不存在");
        }
        return interview;
    }

    private InterviewListVO toListVo(Interview interview) {
        InterviewListVO vo = new InterviewListVO();
        vo.setInterviewId(interview.getId());
        vo.setApplicationId(interview.getApplicationId());
        vo.setJobId(interview.getJobId());
        vo.setCandidateId(interview.getCandidateId());
        vo.setCandidateName(interview.getCandidateName());
        vo.setJobTitle(interview.getJobTitle());
        vo.setInterviewerId(interview.getInterviewerId());
        vo.setInterviewerName(interview.getInterviewerName());
        vo.setRoundNo(interview.getRoundNo());
        vo.setRoundType(interview.getRoundType());
        vo.setRoundTypeLabel(InterviewConstants.roundTypeLabel(interview.getRoundType()));
        vo.setInterviewMode(interview.getInterviewMode());
        vo.setInterviewModeLabel(InterviewConstants.interviewModeLabel(interview.getInterviewMode()));
        vo.setScheduledStart(interview.getScheduledStart());
        vo.setScheduledEnd(interview.getScheduledEnd());
        vo.setMeetingUrl(interview.getMeetingUrl());
        vo.setLocation(interview.getLocation());
        vo.setStatus(interview.getStatus());
        vo.setStatusLabel(InterviewConstants.statusLabel(interview.getStatus()));
        vo.setTotalScore(interview.getTotalScore());
        return vo;
    }

    private InterviewDetailVO toDetailVo(Interview interview) {
        InterviewDetailVO vo = new InterviewDetailVO();
        InterviewListVO base = toListVo(interview);
        vo.setInterviewId(base.getInterviewId());
        vo.setApplicationId(base.getApplicationId());
        vo.setJobId(base.getJobId());
        vo.setCandidateId(base.getCandidateId());
        vo.setCandidateName(base.getCandidateName());
        vo.setJobTitle(base.getJobTitle());
        vo.setInterviewerId(base.getInterviewerId());
        vo.setInterviewerName(base.getInterviewerName());
        vo.setRoundNo(base.getRoundNo());
        vo.setRoundType(base.getRoundType());
        vo.setRoundTypeLabel(base.getRoundTypeLabel());
        vo.setInterviewMode(base.getInterviewMode());
        vo.setInterviewModeLabel(base.getInterviewModeLabel());
        vo.setScheduledStart(base.getScheduledStart());
        vo.setScheduledEnd(base.getScheduledEnd());
        vo.setMeetingUrl(base.getMeetingUrl());
        vo.setLocation(base.getLocation());
        vo.setStatus(base.getStatus());
        vo.setStatusLabel(base.getStatusLabel());
        vo.setTotalScore(base.getTotalScore());
        vo.setCreatedBy(interview.getCreatedBy());
        vo.setCreatedByName(interview.getCreatedByName());
        vo.setCreatedAt(interview.getCreatedAt());
        vo.setUpdatedAt(interview.getUpdatedAt());
        InterviewEvaluationVO evaluation =
                InterviewEvaluationServiceImpl.toVo(evaluationService.findByInterviewId(interview.getId()));
        vo.setEvaluation(evaluation);
        return vo;
    }

    private static String defaultName(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveDisplayName(Long userId, String headerName) {
        if (StringUtils.hasText(headerName)) {
            return headerName.trim();
        }
        if (userId == null) {
            return "用户";
        }
        try {
            Map<String, Object> user = loadUserBrief(userId);
            return defaultName(FeignResponseHelper.strVal(user.get("nickname")), "用户");
        } catch (Exception e) {
            return "用户";
        }
    }
}
