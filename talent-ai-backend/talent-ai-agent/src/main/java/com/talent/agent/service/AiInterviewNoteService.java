package com.talent.agent.service;

import com.talent.agent.domain.dto.InterviewNoteSaveRequest;
import com.talent.agent.domain.dto.InterviewNoteSynthesizeRequest;
import com.talent.agent.domain.vo.InterviewNoteVO;

public interface AiInterviewNoteService {

    InterviewNoteVO getByInterview(Long interviewId, Long interviewerId);

    InterviewNoteVO save(InterviewNoteSaveRequest request, Long interviewerId);

    InterviewNoteVO synthesize(InterviewNoteSynthesizeRequest request, Long interviewerId);
}
