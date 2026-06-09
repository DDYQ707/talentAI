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
}
