package com.talent.agent.service;

import com.talent.agent.domain.dto.ParseTaskRequest;

public interface ParseTaskContextService {

    ParseTaskRequest buildFromResumeContext(
            Long resumeId, Long applicationId, Long jobId, Long candidateId);
}
