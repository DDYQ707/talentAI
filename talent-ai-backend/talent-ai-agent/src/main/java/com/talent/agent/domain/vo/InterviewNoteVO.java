package com.talent.agent.domain.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewNoteVO {

    private Long id;

    private Long interviewId;

    private Long interviewerId;

    private String noteContent;

    private String aiSummary;

    private Integer aiSuggestedScore;

    private Integer aiSuggestedConclusion;

    private String aiSuggestedConclusionLabel;

    private Map<String, Integer> aiDimensionScores;

    private List<String> aiHighlights;

    private LocalDateTime updatedAt;
}
