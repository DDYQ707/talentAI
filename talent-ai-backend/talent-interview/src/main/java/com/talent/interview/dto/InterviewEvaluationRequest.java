package com.talent.interview.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewEvaluationRequest {

    private BigDecimal overallScore;

    /** 1-通过 2-待定 3-不通过 */
    private Integer conclusion;

    private String comment;

    private Map<String, Object> dimensionScores;
}
