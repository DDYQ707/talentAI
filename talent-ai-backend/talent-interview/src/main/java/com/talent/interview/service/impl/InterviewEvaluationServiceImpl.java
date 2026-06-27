package com.talent.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.interview.constant.InterviewConstants;
import com.talent.interview.dto.InterviewEvaluationRequest;
import com.talent.interview.entity.Interview;
import com.talent.interview.entity.InterviewEvaluation;
import com.talent.interview.feign.AiAgentFeignClient;
import com.talent.interview.feign.JobFeignClient;
import com.talent.interview.mapper.InterviewEvaluationMapper;
import com.talent.interview.mapper.InterviewMapper;
import com.talent.interview.service.InterviewEvaluationService;
import com.talent.interview.util.FeignResponseHelper;
import com.talent.interview.vo.InterviewEvaluationVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewEvaluationServiceImpl implements InterviewEvaluationService {

    private final InterviewEvaluationMapper evaluationMapper;
    private final InterviewMapper interviewMapper;
    private final ObjectMapper objectMapper;
    private final JobFeignClient jobFeignClient;
    private final AiAgentFeignClient aiAgentFeignClient;

    @Override
    public InterviewEvaluation findByInterviewId(Long interviewId) {
        if (interviewId == null) {
            return null;
        }
        return evaluationMapper.selectOne(
                new LambdaQueryWrapper<InterviewEvaluation>()
                        .eq(InterviewEvaluation::getInterviewId, interviewId)
                        .orderByDesc(InterviewEvaluation::getCreatedAt)
                        .last("LIMIT 1"));
    }

    @Override
    public Map<Long, InterviewEvaluation> findMapByInterviewIds(Collection<Long> interviewIds) {
        if (interviewIds == null || interviewIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = interviewIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<InterviewEvaluation> rows = evaluationMapper.selectList(
                new LambdaQueryWrapper<InterviewEvaluation>()
                        .in(InterviewEvaluation::getInterviewId, ids)
                        .orderByDesc(InterviewEvaluation::getCreatedAt));
        Map<Long, InterviewEvaluation> result = new HashMap<>();
        for (InterviewEvaluation row : rows) {
            result.putIfAbsent(row.getInterviewId(), row);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewEvaluationVO submitEvaluation(
            Long interviewId, Long evaluatorId, String evaluatorName, InterviewEvaluationRequest request) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("评价内容不能为空");
        }
        if (!InterviewConstants.isValidConclusion(request.getConclusion())) {
            throw new IllegalArgumentException("评价结论无效，应为 1-通过 2-待定 3-不通过");
        }

        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new IllegalArgumentException("面试记录不存在");
        }
        if (interview.getStatus() == null || interview.getStatus() != InterviewConstants.STATUS_PENDING) {
            throw new IllegalArgumentException("仅待面试的面试可提交评价");
        }
        if (evaluatorId == null || !evaluatorId.equals(interview.getInterviewerId())) {
            throw new IllegalArgumentException("无权评价该面试");
        }

        InterviewEvaluation existing = findByInterviewId(interviewId);
        if (existing != null) {
            throw new IllegalArgumentException("该面试已提交评价，不可重复提交");
        }

        Map<String, BigDecimal> dimensionScores = validateAndNormalizeDimensions(request.getDimensionScores());
        BigDecimal overallScore = dimensionScores.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dimensionScores.size()), 1, RoundingMode.HALF_UP);

        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setInterviewId(interviewId);
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setEvaluatorName(resolveEvaluatorName(evaluatorName, interview.getInterviewerName()));
        evaluation.setOverallScore(overallScore);
        evaluation.setConclusion(request.getConclusion());
        evaluation.setComment(StringUtils.hasText(request.getComment()) ? request.getComment().trim() : null);
        evaluation.setDimensionScores(toJson(dimensionScores));
        evaluation.setCreatedAt(LocalDateTime.now());
        evaluation.setUpdatedAt(LocalDateTime.now());
        evaluationMapper.insert(evaluation);

        interview.setTotalScore(overallScore);
        interview.setStatus(InterviewConstants.STATUS_COMPLETED);
        interview.setUpdatedAt(LocalDateTime.now());
        interviewMapper.updateById(interview);

        triggerAiProfileAfterEvaluation(interview);

        return toVo(evaluation);
    }

    private Map<String, BigDecimal> validateAndNormalizeDimensions(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new IllegalArgumentException("请填写沟通能力、专业技能、岗位匹配度三项评分");
        }
        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        for (String key : InterviewConstants.REQUIRED_EVALUATION_DIMENSIONS) {
            Object value = raw.get(key);
            if (value == null) {
                throw new IllegalArgumentException("缺少评分项：" + key);
            }
            BigDecimal score;
            try {
                score = new BigDecimal(String.valueOf(value));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("评分项「" + key + "」格式无效");
            }
            if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("评分项「" + key + "」应在 0～100 之间");
            }
            normalized.put(key, score);
        }
        return normalized;
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

    private String resolveEvaluatorName(String evaluatorName, String interviewerName) {
        if (StringUtils.hasText(evaluatorName)) {
            return evaluatorName.trim();
        }
        if (StringUtils.hasText(interviewerName)) {
            return interviewerName.trim();
        }
        return "面试官";
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("维度评分格式无效");
        }
    }

    public static InterviewEvaluationVO toVo(InterviewEvaluation evaluation) {
        if (evaluation == null) {
            return null;
        }
        InterviewEvaluationVO vo = new InterviewEvaluationVO();
        vo.setEvaluationId(evaluation.getId());
        vo.setEvaluatorId(evaluation.getEvaluatorId());
        vo.setEvaluatorName(evaluation.getEvaluatorName());
        vo.setDimensionScores(evaluation.getDimensionScores());
        vo.setOverallScore(evaluation.getOverallScore());
        vo.setConclusion(evaluation.getConclusion());
        vo.setConclusionLabel(InterviewConstants.conclusionLabel(evaluation.getConclusion()));
        vo.setComment(evaluation.getComment());
        vo.setCreatedAt(evaluation.getCreatedAt());
        return vo;
    }
}
