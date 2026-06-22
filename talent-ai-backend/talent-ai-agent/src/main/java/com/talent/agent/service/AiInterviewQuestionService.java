package com.talent.agent.service;

import com.talent.agent.domain.dto.InterviewQuestionGenerateRequest;
import com.talent.agent.domain.vo.InterviewQuestionGenerateResultVO;
import com.talent.agent.domain.vo.InterviewQuestionVO;
import java.util.List;

public interface AiInterviewQuestionService {

    InterviewQuestionGenerateResultVO generate(InterviewQuestionGenerateRequest request);

    List<InterviewQuestionVO> listByInterviewId(Long interviewId);
}
