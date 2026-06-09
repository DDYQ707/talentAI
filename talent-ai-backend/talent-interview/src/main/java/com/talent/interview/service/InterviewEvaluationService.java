package com.talent.interview.service;

import com.talent.interview.dto.InterviewEvaluationRequest;
import com.talent.interview.entity.InterviewEvaluation;
import com.talent.interview.vo.InterviewEvaluationVO;

public interface InterviewEvaluationService {

    InterviewEvaluation findByInterviewId(Long interviewId);

    InterviewEvaluationVO submitEvaluation(
            Long interviewId, Long evaluatorId, String evaluatorName, InterviewEvaluationRequest request);
}
