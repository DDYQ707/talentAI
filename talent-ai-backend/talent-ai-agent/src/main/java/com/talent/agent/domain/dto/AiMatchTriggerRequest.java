package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class AiMatchTriggerRequest {

    private Long resumeId;

    private Long jobId;

    private Long applicationId;

    private Long candidateId;
}
