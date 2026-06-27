package com.talent.interview.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewListVO {

    private Long interviewId;

    private Long applicationId;

    private Long jobId;

    private Long candidateId;

    private String candidateName;

    private String jobTitle;

    private Long interviewerId;

    private String interviewerName;

    private Integer roundNo;

    private Integer roundType;

    private String roundTypeLabel;

    private Integer interviewMode;

    private String interviewModeLabel;

    private LocalDateTime scheduledStart;

    private LocalDateTime scheduledEnd;

    private String meetingUrl;

    private String location;

    private Integer status;

    private String statusLabel;

    private BigDecimal totalScore;

    /** 评价结论：1-通过 2-待定 3-不通过（仅已完成面试可能有值） */
    private Integer evaluationConclusion;

    private String evaluationConclusionLabel;

    /** 面试官评价详情（已完成且有评价时返回） */
    private InterviewEvaluationVO evaluation;

    /** 关联简历 ID（HR 列表跳转详情用） */
    private Long resumeId;
}
