package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class AiParseRetryRequest {

    private Long resumeId;

    private Long applicationId;

    private Long jobId;

    private Long candidateId;
}
