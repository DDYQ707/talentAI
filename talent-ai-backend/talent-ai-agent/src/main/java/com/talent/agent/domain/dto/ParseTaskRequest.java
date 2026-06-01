package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class ParseTaskRequest {

    private Long attachmentId;

    private Long resumeId;

    private Long modelId;
}
