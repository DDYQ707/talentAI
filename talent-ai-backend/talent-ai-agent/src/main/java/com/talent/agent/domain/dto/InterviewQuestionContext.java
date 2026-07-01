package com.talent.agent.domain.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 面试题生成时的轮次与历史上下文 */
@Getter
@Builder
public class InterviewQuestionContext {

    private Long interviewId;
    private Integer roundNo;
    private Integer roundType;
    private String roundTypeLabel;
    private List<String> previousQuestionTexts;
    private List<String> previousRoundSummaries;
}
