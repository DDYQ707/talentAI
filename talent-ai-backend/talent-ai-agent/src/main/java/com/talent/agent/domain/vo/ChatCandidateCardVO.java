package com.talent.agent.domain.vo;

import lombok.Data;

@Data
public class ChatCandidateCardVO {

    private Long resumeId;

    private Long candidateId;

    private String candidateName;

    private String resumeName;

    private String currentTitle;

    private String city;

    private String appliedJobTitle;

    private Integer matchScore;

    private Integer screenStatus;

    private Long attachmentId;

    private Long applicationId;
}
