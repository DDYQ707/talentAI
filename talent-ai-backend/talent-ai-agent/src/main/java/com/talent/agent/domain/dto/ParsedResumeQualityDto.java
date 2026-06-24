package com.talent.agent.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ParsedResumeQualityDto {

    private Integer qualityScore;

    private String summary;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> suggestions;

    private Map<String, Integer> dimensionScores;
}
