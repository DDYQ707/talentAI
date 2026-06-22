package com.talent.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.interview.constant.InterviewConstants;
import com.talent.interview.dto.InterviewEvaluationRequest;
import com.talent.interview.entity.Interview;
import com.talent.interview.entity.InterviewEvaluation;
import com.talent.interview.mapper.InterviewEvaluationMapper;
import com.talent.interview.mapper.InterviewMapper;
import com.talent.interview.service.InterviewEvaluationService;
import com.talent.interview.vo.InterviewEvaluationVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InterviewEvaluationServiceImpl implements InterviewEvaluationService {

    private final InterviewEvaluationMapper evaluationMapper;
    private final InterviewMapper interviewMapper;
    private final ObjectMapper objectMapper;

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
    @Transactional(rollbackFor = Exception.class)
    public InterviewEvaluationVO submitEvaluation(
            Long interviewId, Long evaluatorId, String evaluatorName, InterviewEvaluationRequest request) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("评价内容不能为空");
        }
        if (request.getOverallScore() == null) {
            throw new IllegalArgumentException("综合评分不能为空");
        }
        if (!InterviewConstants.isValidConclusion(request.getConclusion())) {
            throw new IllegalArgumentException("评价结论无效，应为 1-通过 2-待定 3-不通过");
        }

        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            throw new IllegalArgumentException("面试记录不存在");
        }
        if (interview.getStatus() == null || interview.getStatus() != InterviewConstants.STATUS_PENDING) {
            throw new IllegalArgumentException("仅待进行的面试可提交评价");
        }
        if (evaluatorId == null || !evaluatorId.equals(interview.getInterviewerId())) {
            throw new IllegalArgumentException("无权评价该面试");
        }

        InterviewEvaluation existing = findByInterviewId(interviewId);
        if (existing != null) {
            throw new IllegalArgumentException("该面试已提交评价，不可重复提交");
        }

        BigDecimal score = request.getOverallScore();
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("综合评分应在 0～100 之间");
        }

        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setInterviewId(interviewId);
        evaluation.setEvaluatorId(evaluatorId);
        evaluation.setEvaluatorName(resolveEvaluatorName(evaluatorName, interview.getInterviewerName()));
        evaluation.setOverallScore(score);
        evaluation.setConclusion(request.getConclusion());
        evaluation.setComment(StringUtils.hasText(request.getComment()) ? request.getComment().trim() : null);
        evaluation.setDimensionScores(toJson(request.getDimensionScores()));
        evaluation.setCreatedAt(LocalDateTime.now());
        evaluation.setUpdatedAt(LocalDateTime.now());
        evaluationMapper.insert(evaluation);

        interview.setTotalScore(score);
        interview.setStatus(InterviewConstants.STATUS_COMPLETED);
        interview.setUpdatedAt(LocalDateTime.now());
        interviewMapper.updateById(interview);

        return toVo(evaluation);
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
