package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class MatchRequest {

    private Long applicationId;

    private Long jobId;

    private Long resumeId;

    private Long modelId;
}
