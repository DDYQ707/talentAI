package com.talent.interview.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewEvaluationVO {

    private Long evaluationId;

    private Long evaluatorId;

    private String evaluatorName;

    private String dimensionScores;

    private BigDecimal overallScore;

    private Integer conclusion;

    private String conclusionLabel;

    private String comment;

    private LocalDateTime createdAt;

    /** 关联面试（供 AI 递进出题等内部场景） */
    private Long interviewId;

    private Integer roundNo;

    private String roundTypeLabel;
}
