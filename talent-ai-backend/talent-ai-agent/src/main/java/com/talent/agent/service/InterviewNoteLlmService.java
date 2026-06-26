package com.talent.agent.service;

import com.talent.agent.domain.dto.ParsedInterviewEvaluationDto;
import com.talent.agent.domain.vo.MatchResultVO;
import java.util.List;

public interface InterviewNoteLlmService {

    ParsedInterviewEvaluationDto synthesize(
            String candidateName,
            String jobTitle,
            MatchResultVO matchResult,
            List<String> interviewQuestions,
            String noteContent);
}
