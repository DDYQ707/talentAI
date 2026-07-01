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
import com.talent.interview.service.InterviewAsyncSideEffectService;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.talent.interview.entity.InterviewEvaluation;
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
    private static final int SCREEN_STATUS_PENDING = 1;
    private static final int SCREEN_STATUS_INTERVIEWING = 2;
    private static final int SCREEN_STATUS_OFFER_PENDING = 5;

    private final InterviewMapper interviewMapper;
    private final InterviewEvaluationService evaluationService;
    private final InterviewAsyncSideEffectService asyncSideEffectService;
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
    public List<InterviewEvaluationVO> listEvaluationsByApplication(Long applicationId) {
        return listByApplicationId(applicationId).stream()
                .map(interview -> {
                    InterviewEvaluationVO vo = InterviewEvaluationServiceImpl.toVo(
                            evaluationService.findByInterviewId(interview.getId()));
                    if (vo == null) {
                        return null;
                    }
                    vo.setInterviewId(interview.getId());
                    vo.setRoundNo(interview.getRoundNo());
                    vo.setRoundTypeLabel(InterviewConstants.roundTypeLabel(interview.getRoundType()));
                    return vo;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
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
        validateRoundSchedule(roundNo, request.getRoundType());

        long duplicate = interviewMapper.selectCount(
                new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getApplicationId, request.getApplicationId())
                        .eq(Interview::getRoundNo, roundNo)
                        .in(
                                Interview::getStatus,
                                InterviewConstants.STATUS_PENDING,
                                InterviewConstants.STATUS_TO_SCHEDULE));
        if (duplicate > 0) {
            throw new IllegalArgumentException("该投递的第 " + roundNo + " 轮面试已有待进行安排，请勿重复安排");
        }

        completePreviousPendingRounds(request.getApplicationId(), roundNo);

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
        notifyInterviewScheduled(interview);

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
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pageForHr(
            String role, Integer page, Integer size, String keyword, Integer status,
            LocalDate dateFrom, LocalDate dateTo) {
        InterviewAuthSupport.requireHr(role);
        repairSupersededPendingInterviews();
        long current = page != null && page > 0 ? page : 1;
        long pageSize = size != null && size > 0 ? Math.min(size, 100) : 10;

        LambdaQueryWrapper<Interview> wrapper = buildListWrapper(keyword, status, dateFrom, dateTo, null, null);
        wrapper.orderByDesc(Interview::getScheduledStart);

        Page<Interview> pageResult = interviewMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Map<Long, Long> resumeIdCache = new HashMap<>();
        List<Long> completedInterviewIds = pageResult.getRecords().stream()
                .filter(i -> Objects.equals(i.getStatus(), InterviewConstants.STATUS_COMPLETED))
                .map(Interview::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, InterviewEvaluation> evaluationMap =
                evaluationService.findMapByInterviewIds(completedInterviewIds);
        Map<Long, Map<String, Object>> offerMap = loadOfferMapByApplications(
                pageResult.getRecords().stream()
                        .map(Interview::getApplicationId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList()));
        Map<Long, Map<String, Object>> applicationCache = new HashMap<>();
        List<InterviewListVO> records = pageResult.getRecords().stream()
                .map(interview -> toHrListVo(interview, resumeIdCache, evaluationMap, offerMap, applicationCache))
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
            throw new IllegalArgumentException("仅待面试的面试可取消");
        }
        interview.setStatus(InterviewConstants.STATUS_CANCELLED);
        interview.setUpdatedAt(LocalDateTime.now());
        interviewMapper.updateById(interview);

        if (!hasOtherPendingInterview(interview.getCandidateId(), interview.getId())) {
            asyncSideEffectService.revertToPendingScreenAfterCancel(interview);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignForHr(String role, Long hrId, String hrName, Long interviewId, Long newInterviewerId) {
        InterviewAuthSupport.requireHr(role);
        InterviewAuthSupport.requireUserId(hrId);
        if (newInterviewerId == null || newInterviewerId <= 0) {
            throw new IllegalArgumentException("请选择新的面试官");
        }

        Interview interview = requireInterview(interviewId);
        if (interview.getStatus() == null || interview.getStatus() != InterviewConstants.STATUS_PENDING) {
            throw new IllegalArgumentException("仅待面试的面试可改派面试官");
        }
        if (newInterviewerId.equals(interview.getInterviewerId())) {
            throw new IllegalArgumentException("新面试官与当前面试官相同，无需改派");
        }

        Map<String, Object> interviewer = loadUserBrief(newInterviewerId);
        Integer userType = FeignResponseHelper.intVal(interviewer.get("userType"));
        if (userType == null || userType != USER_TYPE_INTERVIEWER) {
            throw new IllegalArgumentException("所选用户不是面试官账号");
        }

        interview.setInterviewerId(newInterviewerId);
        interview.setInterviewerName(defaultName(FeignResponseHelper.strVal(interviewer.get("nickname")), "面试官"));
        interview.setUpdatedAt(LocalDateTime.now());
        interviewMapper.updateById(interview);
        notifyInterviewerReassigned(interview);
    }

    @Override
    public List<InterviewListVO> listByApplicationForHr(String role, Long applicationId) {
        InterviewAuthSupport.requireHr(role);
        if (applicationId == null) {
            throw new IllegalArgumentException("applicationId 不能为空");
        }
        List<Interview> interviews = listByApplicationId(applicationId);
        List<Long> completedIds = interviews.stream()
                .filter(i -> Objects.equals(i.getStatus(), InterviewConstants.STATUS_COMPLETED))
                .map(Interview::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, InterviewEvaluation> evaluationMap =
                evaluationService.findMapByInterviewIds(completedIds);
        Map<Long, Map<String, Object>> offerMap = loadOfferMapByApplications(
                interviews.stream()
                        .map(Interview::getApplicationId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList()));
        Map<Long, Map<String, Object>> applicationCache = new HashMap<>();
        return interviews.stream()
                .map(interview -> toHrListVo(interview, new HashMap<>(), evaluationMap, offerMap, applicationCache))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> pageForInterviewer(
            String role, Long interviewerId, Integer page, Integer size, String keyword, Integer status) {
        InterviewAuthSupport.requireInterviewer(role);
        InterviewAuthSupport.requireUserId(interviewerId);

        long current = page != null && page > 0 ? page : 1;
        long pageSize = size != null && size > 0 ? Math.min(size, 100) : 10;

        LambdaQueryWrapper<Interview> wrapper = buildListWrapper(keyword, status, null, null, interviewerId, null);
        wrapper.orderByAsc(Interview::getScheduledStart);

        Page<Interview> pageResult = interviewMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<Long> completedIds = pageResult.getRecords().stream()
                .filter(i -> Objects.equals(i.getStatus(), InterviewConstants.STATUS_COMPLETED))
                .map(Interview::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, InterviewEvaluation> evaluationMap =
                evaluationService.findMapByInterviewIds(completedIds);
        Map<Long, Map<String, Object>> applicationCache = new HashMap<>();
        List<InterviewListVO> records = pageResult.getRecords().stream()
                .map(interview -> toInterviewerListVo(interview, evaluationMap, applicationCache))
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

    @Override
    public Map<String, Object> pageForCandidate(
            String role, Long candidateId, Integer page, Integer size, String keyword, Integer status) {
        InterviewAuthSupport.requireCandidate(role);
        InterviewAuthSupport.requireUserId(candidateId);

        long current = page != null && page > 0 ? page : 1;
        long pageSize = size != null && size > 0 ? Math.min(size, 100) : 10;

        LambdaQueryWrapper<Interview> wrapper = buildListWrapper(keyword, status, null, null, null, candidateId);
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
    public InterviewDetailVO detailForCandidate(String role, Long candidateId, Long interviewId) {
        InterviewAuthSupport.requireCandidate(role);
        InterviewAuthSupport.requireUserId(candidateId);
        Interview interview = requireInterview(interviewId);
        if (!candidateId.equals(interview.getCandidateId())) {
            throw new IllegalArgumentException("无权查看该面试");
        }
        return toCandidateDetailVo(interview);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncOnApplicationRejected(Long applicationId) {
        if (applicationId == null) {
            return;
        }
        List<Interview> pending = interviewMapper.selectList(
                new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getApplicationId, applicationId)
                        .in(
                                Interview::getStatus,
                                InterviewConstants.STATUS_PENDING,
                                InterviewConstants.STATUS_TO_SCHEDULE));
        if (pending.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Interview interview : pending) {
            interview.setStatus(InterviewConstants.STATUS_CANCELLED);
            interview.setUpdatedAt(now);
            interviewMapper.updateById(interview);
            log.info(
                    "HR 淘汰联动取消待面试 applicationId={} interviewId={}",
                    applicationId,
                    interview.getId());
        }
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

    /** 轮次序号须在 1～3；面试类型由 HR 自由选择，不要求按固定顺序安排 */
    private void validateRoundSchedule(int roundNo, Integer roundType) {
        if (roundNo < 1 || roundNo > InterviewConstants.MAX_INTERVIEW_ROUNDS) {
            throw new IllegalArgumentException(
                    "轮次序号须在 1～"
                            + InterviewConstants.MAX_INTERVIEW_ROUNDS
                            + " 之间，每份投递最多安排 "
                            + InterviewConstants.MAX_INTERVIEW_ROUNDS
                            + " 轮");
        }
        if (roundType != null && !InterviewConstants.isValidRoundType(roundType)) {
            throw new IllegalArgumentException("roundType 无效，应为 1～7");
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

    private boolean hasOtherPendingInterview(Long candidateId, Long excludeInterviewId) {
        if (candidateId == null) {
            return false;
        }
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<Interview>()
                .eq(Interview::getCandidateId, candidateId)
                .eq(Interview::getStatus, InterviewConstants.STATUS_PENDING);
        if (excludeInterviewId != null) {
            wrapper.ne(Interview::getId, excludeInterviewId);
        }
        return interviewMapper.selectCount(wrapper) > 0;
    }

    /**
     * 安排更高轮次时，将同投递下更早轮次仍为「待面试」的记录标记为「面试完成」。
     * 复试等新面试单独一条记录，前一轮不应继续显示待面试。
     */
    private void completePreviousPendingRounds(Long applicationId, int newRoundNo) {
        if (applicationId == null || newRoundNo <= 1) {
            return;
        }
        List<Interview> previousPending = interviewMapper.selectList(
                new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getApplicationId, applicationId)
                        .lt(Interview::getRoundNo, newRoundNo)
                        .eq(Interview::getStatus, InterviewConstants.STATUS_PENDING));
        if (previousPending.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Interview previous : previousPending) {
            previous.setStatus(InterviewConstants.STATUS_COMPLETED);
            previous.setUpdatedAt(now);
            interviewMapper.updateById(previous);
            log.info(
                    "安排第 {} 轮时将第 {} 轮标记为面试完成 applicationId={} interviewId={}",
                    newRoundNo,
                    previous.getRoundNo(),
                    applicationId,
                    previous.getId());
        }
    }

    /** 修复历史数据：同投递已存在更高轮次时，低轮次不应仍为待面试 */
    private void repairSupersededPendingInterviews() {
        List<Interview> pendingList = interviewMapper.selectList(
                new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getStatus, InterviewConstants.STATUS_PENDING)
                        .isNotNull(Interview::getApplicationId)
                        .isNotNull(Interview::getRoundNo));
        if (pendingList.isEmpty()) {
            return;
        }
        Map<Long, Integer> maxRoundByApp = new HashMap<>();
        for (Long applicationId :
                pendingList.stream().map(Interview::getApplicationId).distinct().toList()) {
            Interview maxInterview = interviewMapper.selectOne(
                    new LambdaQueryWrapper<Interview>()
                            .eq(Interview::getApplicationId, applicationId)
                            .ne(Interview::getStatus, InterviewConstants.STATUS_CANCELLED)
                            .orderByDesc(Interview::getRoundNo)
                            .last("LIMIT 1"),
                    false);
            if (maxInterview != null && maxInterview.getRoundNo() != null) {
                maxRoundByApp.put(applicationId, maxInterview.getRoundNo());
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (Interview pending : pendingList) {
            Integer maxRound = maxRoundByApp.get(pending.getApplicationId());
            if (maxRound != null
                    && pending.getRoundNo() != null
                    && maxRound > pending.getRoundNo()) {
                pending.setStatus(InterviewConstants.STATUS_COMPLETED);
                pending.setUpdatedAt(now);
                interviewMapper.updateById(pending);
                log.info(
                        "修复已被更高轮次取代的待面试记录 applicationId={} roundNo={} interviewId={}",
                        pending.getApplicationId(),
                        pending.getRoundNo(),
                        pending.getId());
            }
        }
    }

    private Long resolveResumeId(Interview interview) {
        return resolveResumeId(interview, new HashMap<>());
    }

    private LambdaQueryWrapper<Interview> buildListWrapper(
            String keyword, Integer status, LocalDate dateFrom, LocalDate dateTo,
            Long interviewerId, Long candidateId) {
        LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
        if (interviewerId != null) {
            wrapper.eq(Interview::getInterviewerId, interviewerId);
        }
        if (candidateId != null) {
            wrapper.eq(Interview::getCandidateId, candidateId);
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

    private InterviewListVO toInterviewerListVo(
            Interview interview,
            Map<Long, InterviewEvaluation> evaluationMap,
            Map<Long, Map<String, Object>> applicationCache) {
        InterviewListVO vo = toListVo(interview);
        applyEvaluationSummary(vo, interview, evaluationMap);
        applyRecruitmentOutcome(vo, interview, applicationCache);
        return vo;
    }

    private void applyRecruitmentOutcome(
            InterviewListVO vo, Interview interview, Map<Long, Map<String, Object>> applicationCache) {
        if (interview.getApplicationId() == null) {
            return;
        }
        Map<String, Object> appData = loadApplicationData(interview.getApplicationId(), applicationCache);
        if (appData == null) {
            return;
        }
        Integer applicationStatus = FeignResponseHelper.intVal(appData.get("status"));
        vo.setApplicationStatus(applicationStatus);
        vo.setRecruitmentOutcomeLabel(recruitmentOutcomeLabel(applicationStatus));
    }

    private Map<String, Object> loadApplicationData(
            Long applicationId, Map<Long, Map<String, Object>> applicationCache) {
        if (applicationId == null) {
            return null;
        }
        if (applicationCache != null && applicationCache.containsKey(applicationId)) {
            return applicationCache.get(applicationId);
        }
        try {
            Map<String, Object> res = jobFeignClient.applicationById(applicationId);
            if (FeignResponseHelper.code(res) != 200) {
                return null;
            }
            Map<String, Object> data = FeignResponseHelper.dataMap(res);
            if (applicationCache != null) {
                applicationCache.put(applicationId, data);
            }
            return data;
        } catch (Exception e) {
            log.warn("查询投递状态失败 applicationId={}", applicationId, e);
            return null;
        }
    }

    private static String recruitmentOutcomeLabel(Integer applicationStatus) {
        if (applicationStatus == null) {
            return null;
        }
        return switch (applicationStatus) {
            case 2 -> "已录用";
            case 3 -> "HR已淘汰";
            default -> null;
        };
    }

    /** HR 面试列表：附带评价结论、resumeId 与 Offer 状态 */
    private InterviewListVO toHrListVo(
            Interview interview,
            Map<Long, Long> resumeIdCache,
            Map<Long, InterviewEvaluation> evaluationMap,
            Map<Long, Map<String, Object>> offerMap,
            Map<Long, Map<String, Object>> applicationCache) {
        InterviewListVO vo = toListVo(interview);
        applyEvaluationSummary(vo, interview, evaluationMap);
        applyRecruitmentOutcome(vo, interview, applicationCache);
        if (interview.getApplicationId() != null || interview.getCandidateId() != null) {
            vo.setResumeId(resolveResumeId(interview, resumeIdCache));
        }
        if (interview.getApplicationId() != null && offerMap != null) {
            Map<String, Object> offerBrief = offerMap.get(interview.getApplicationId());
            if (offerBrief != null) {
                vo.setOfferId(FeignResponseHelper.longVal(offerBrief.get("offerId")));
                vo.setOfferStatus(FeignResponseHelper.intVal(offerBrief.get("status")));
                vo.setOfferStatusText(FeignResponseHelper.strVal(offerBrief.get("statusText")));
            }
        }
        return vo;
    }

    private InterviewListVO toHrListVo(
            Interview interview,
            Map<Long, Long> resumeIdCache,
            Map<Long, InterviewEvaluation> evaluationMap,
            Map<Long, Map<String, Object>> offerMap) {
        return toHrListVo(interview, resumeIdCache, evaluationMap, offerMap, new HashMap<>());
    }

    private Map<Long, Map<String, Object>> loadOfferMapByApplications(List<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("applicationIds", applicationIds);
            Map<String, Object> res = jobFeignClient.latestOfferByApplications(body);
            if (FeignResponseHelper.code(res) != 200) {
                log.warn("批量查询 Offer 状态失败 msg={}", FeignResponseHelper.msg(res, ""));
                return Map.of();
            }
            Object itemsObj = res.get("items");
            if (!(itemsObj instanceof Map<?, ?> items)) {
                return Map.of();
            }
            Map<Long, Map<String, Object>> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : items.entrySet()) {
                Long applicationId = FeignResponseHelper.longVal(entry.getKey());
                if (applicationId == null || !(entry.getValue() instanceof Map<?, ?> briefRaw)) {
                    continue;
                }
                Map<String, Object> brief = new HashMap<>();
                briefRaw.forEach((k, v) -> brief.put(String.valueOf(k), v));
                result.put(applicationId, brief);
            }
            return result;
        } catch (Exception e) {
            log.warn("批量查询 Offer 状态异常 error={}", e.getMessage());
            return Map.of();
        }
    }

    /** HR 面试列表：附带评价结论与 resumeId */
    private InterviewListVO toHrListVo(
            Interview interview, Map<Long, Long> resumeIdCache, Map<Long, InterviewEvaluation> evaluationMap) {
        return toHrListVo(interview, resumeIdCache, evaluationMap, Map.of());
    }

    private void applyEvaluationSummary(
            InterviewListVO vo, Interview interview, Map<Long, InterviewEvaluation> evaluationMap) {
        if (!Objects.equals(interview.getStatus(), InterviewConstants.STATUS_COMPLETED)) {
            return;
        }
        InterviewEvaluation evaluation = null;
        if (evaluationMap != null && interview.getId() != null) {
            evaluation = evaluationMap.get(interview.getId());
        }
        if (evaluation == null) {
            evaluation = evaluationService.findByInterviewId(interview.getId());
        }
        if (evaluation != null && evaluation.getConclusion() != null) {
            vo.setEvaluationConclusion(evaluation.getConclusion());
            vo.setEvaluationConclusionLabel(InterviewConstants.conclusionLabel(evaluation.getConclusion()));
        }
        if (evaluation != null) {
            vo.setEvaluation(InterviewEvaluationServiceImpl.toVo(evaluation));
            if (vo.getTotalScore() == null && evaluation.getOverallScore() != null) {
                vo.setTotalScore(evaluation.getOverallScore());
            }
        }
    }

    private Long resolveResumeId(Interview interview, Map<Long, Long> resumeIdCache) {
        if (interview == null) {
            return null;
        }
        Long applicationId = interview.getApplicationId();
        Long candidateId = interview.getCandidateId();
        Long cacheId = applicationId != null ? applicationId : candidateId;
        if (cacheId != null && resumeIdCache.containsKey(cacheId)) {
            return resumeIdCache.get(cacheId);
        }

        Long resumeId = null;
        if (applicationId != null) {
            try {
                Map<String, Object> appRes = jobFeignClient.applicationById(applicationId);
                if (FeignResponseHelper.code(appRes) == 200) {
                    resumeId = FeignResponseHelper.longVal(FeignResponseHelper.dataMap(appRes).get("resumeId"));
                }
            } catch (Exception e) {
                log.warn("查询投递 resumeId 失败 applicationId={}", applicationId, e);
            }
        }
        if (resumeId == null && candidateId != null) {
            try {
                Map<String, Object> resumeRes = resumeFeignClient.primaryByCandidate(candidateId);
                if (FeignResponseHelper.code(resumeRes) == 200) {
                    resumeId = FeignResponseHelper.longVal(FeignResponseHelper.dataMap(resumeRes).get("resumeId"));
                }
            } catch (Exception e) {
                log.warn("查询候选人主简历失败 candidateId={}", candidateId, e);
            }
        }

        if (cacheId != null) {
            resumeIdCache.put(cacheId, resumeId);
        }
        return resumeId;
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
        if (evaluation != null && evaluation.getConclusion() != null) {
            vo.setEvaluationConclusion(evaluation.getConclusion());
            vo.setEvaluationConclusionLabel(evaluation.getConclusionLabel());
        }
        applyRecruitmentOutcome(vo, interview, new HashMap<>());
        return vo;
    }

    /** 候选人可见详情：不含面试官评价 */
    private InterviewDetailVO toCandidateDetailVo(Interview interview) {
        InterviewDetailVO vo = toDetailVo(interview);
        vo.setEvaluation(null);
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

    private void notifyInterviewScheduled(Interview interview) {
        if (interview == null) {
            return;
        }
        notifyCandidateInterviewScheduled(interview);
        notifyInterviewerInterviewScheduled(interview);
    }

    private void notifyCandidateInterviewScheduled(Interview interview) {
        if (interview.getCandidateId() == null) {
            return;
        }
        String jobTitle = defaultName(interview.getJobTitle(), "岗位");
        int roundNo = interview.getRoundNo() != null && interview.getRoundNo() > 0 ? interview.getRoundNo() : 1;
        StringBuilder content = new StringBuilder();
        content.append("您投递的「").append(jobTitle).append("」已安排第 ").append(roundNo).append(" 轮面试");
        appendInterviewScheduleDetails(content, interview);
        content.append("，请准时参加。");
        sendInterviewNotification(interview.getCandidateId(), "面试安排通知", content.toString(), interview.getId());
    }

    private void notifyInterviewerInterviewScheduled(Interview interview) {
        if (interview.getInterviewerId() == null) {
            return;
        }
        String candidateName = defaultName(interview.getCandidateName(), "候选人");
        String jobTitle = defaultName(interview.getJobTitle(), "岗位");
        int roundNo = interview.getRoundNo() != null && interview.getRoundNo() > 0 ? interview.getRoundNo() : 1;
        StringBuilder content = new StringBuilder();
        content.append("您有一场新的面试安排：候选人 ")
                .append(candidateName)
                .append("，岗位「")
                .append(jobTitle)
                .append("」，第 ")
                .append(roundNo)
                .append(" 轮");
        appendInterviewScheduleDetails(content, interview);
        content.append("，请提前在「面试准备」中查看候选人信息。");
        sendInterviewNotification(interview.getInterviewerId(), "面试安排通知", content.toString(), interview.getId());
    }

    private void notifyInterviewerReassigned(Interview interview) {
        if (interview == null || interview.getInterviewerId() == null) {
            return;
        }
        String candidateName = defaultName(interview.getCandidateName(), "候选人");
        String jobTitle = defaultName(interview.getJobTitle(), "岗位");
        int roundNo = interview.getRoundNo() != null && interview.getRoundNo() > 0 ? interview.getRoundNo() : 1;
        StringBuilder content = new StringBuilder();
        content.append("HR 已将一场面试改派给您：候选人 ")
                .append(candidateName)
                .append("，岗位「")
                .append(jobTitle)
                .append("」，第 ")
                .append(roundNo)
                .append(" 轮");
        appendInterviewScheduleDetails(content, interview);
        content.append("，请登录面试官端查看详情。");
        sendInterviewNotification(interview.getInterviewerId(), "面试改派通知", content.toString(), interview.getId());
    }

    private void appendInterviewScheduleDetails(StringBuilder content, Interview interview) {
        if (interview.getScheduledStart() != null) {
            content.append("，时间：")
                    .append(interview.getScheduledStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        if (StringUtils.hasText(interview.getLocation())) {
            content.append("，地点：").append(interview.getLocation().trim());
        } else if (StringUtils.hasText(interview.getMeetingUrl())) {
            content.append("，线上会议已安排");
        }
    }

    private void sendInterviewNotification(Long userId, String title, String content, Long interviewId) {
        if (userId == null) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("title", title);
            body.put("content", content);
            body.put("notifyType", (byte) 1);
            body.put("bizType", "interview");
            body.put("bizId", interviewId);
            Map<String, Object> res = authFeignClient.createNotification(body);
            if (res == null) {
                log.warn("创建面试通知无响应 userId={} interviewId={}", userId, interviewId);
                return;
            }
            Object code = res.get("code");
            if (!(code instanceof Number num) || num.intValue() != 200) {
                log.warn("创建面试通知失败 userId={} interviewId={} msg={}", userId, interviewId, res.get("msg"));
            }
        } catch (Exception e) {
            log.warn("创建面试通知异常 userId={} interviewId={} reason={}", userId, interviewId, e.getMessage());
        }
    }
}
