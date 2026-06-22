package com.talent.agent.service;

import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.ParsedInterviewQuestionsDto;
import com.talent.agent.domain.vo.MatchResultVO;

public interface InterviewQuestionLlmService {

    ParsedInterviewQuestionsDto generate(
            JobBriefInfo job,
            String resumeParsedJson,
            MatchResultVO matchResult,
            String candidateName);
}
