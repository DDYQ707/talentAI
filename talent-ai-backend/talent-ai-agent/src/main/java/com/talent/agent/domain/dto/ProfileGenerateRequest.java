package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class ProfileGenerateRequest {

    private Long candidateId;

    private String candidateName;

    private Long resumeId;

    private Long applicationId;

    private Long jobId;

    private String jobTitle;

    /** 投递状态：用于调整画像语气 */
    private Integer status;
}
