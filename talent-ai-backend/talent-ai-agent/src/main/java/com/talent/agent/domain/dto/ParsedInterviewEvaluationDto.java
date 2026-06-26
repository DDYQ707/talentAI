package com.talent.agent.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParsedInterviewEvaluationDto {

    private String summary;

    private Integer suggestedScore;

    /** 1-通过 2-待定 3-不通过 */
    private Integer suggestedConclusion;

    private Map<String, Integer> dimensionScores;

    private List<String> highlights;
}
