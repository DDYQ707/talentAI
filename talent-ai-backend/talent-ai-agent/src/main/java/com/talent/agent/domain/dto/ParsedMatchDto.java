package com.talent.agent.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedMatchDto {

    private Integer matchScore;

    private String matchLevel;

    private String matchReason;

    private List<String> advantages;

    private List<String> disadvantages;

    private List<String> suggestedQuestions;

    private Map<String, Object> dimensionScores;
}
