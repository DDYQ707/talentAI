package com.talent.agent.domain.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ResumeQualityScoreVO {

    private Long scoreId;

    private Long resumeId;

    private Long candidateId;

    private Long parseTaskId;

    private Integer qualityScore;

    private String summary;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> suggestions;

    private Map<String, Integer> dimensionScores;

    private LocalDateTime evaluatedAt;
}
