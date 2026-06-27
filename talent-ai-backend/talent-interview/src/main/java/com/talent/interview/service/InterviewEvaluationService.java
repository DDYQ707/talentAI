package com.talent.interview.service;

import com.talent.interview.dto.InterviewEvaluationRequest;
import com.talent.interview.entity.InterviewEvaluation;
import com.talent.interview.vo.InterviewEvaluationVO;
import java.util.Collection;
import java.util.Map;

public interface InterviewEvaluationService {

    InterviewEvaluation findByInterviewId(Long interviewId);

    Map<Long, InterviewEvaluation> findMapByInterviewIds(Collection<Long> interviewIds);

    InterviewEvaluationVO submitEvaluation(
            Long interviewId, Long evaluatorId, String evaluatorName, InterviewEvaluationRequest request);
}
