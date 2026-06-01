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

    /** 0-待处理 1-处理中 2-成功 3-失败 */
    private Integer matchStatus;

    private String matchLevel;

    private String matchReason;

    private String advantages;

    private String disadvantages;

    private String suggestedQuestions;

    private String dimensionScores;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;
}
