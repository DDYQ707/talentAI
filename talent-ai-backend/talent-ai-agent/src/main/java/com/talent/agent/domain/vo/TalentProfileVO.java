package com.talent.agent.domain.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class TalentProfileVO {

    private Long profileId;

    private Long candidateId;

    private Long applicationId;

    private String profileSummary;

    private List<String> profileTags;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
