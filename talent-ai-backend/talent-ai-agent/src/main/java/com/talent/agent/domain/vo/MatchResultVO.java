package com.talent.agent.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MatchResultVO {

    private Long matchId;

    private Long applicationId;

    private Long jobId;

    private Long resumeId;

    private Integer matchScore;

    private String advantages;

    private String disadvantages;

    private String dimensionScores;

    private LocalDateTime createdAt;
}
