package com.talent.agent.service;

import com.talent.agent.domain.dto.LlmResumeQualityOutcome;

public interface ResumeQualityLlmService {

    LlmResumeQualityOutcome evaluate(String resumeParsedJson);
}
