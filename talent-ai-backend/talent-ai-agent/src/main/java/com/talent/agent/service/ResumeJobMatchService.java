package com.talent.agent.service;

import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.LlmMatchOutcome;

public interface ResumeJobMatchService {

    LlmMatchOutcome match(JobBriefInfo job, String resumeParsedJson);
}
