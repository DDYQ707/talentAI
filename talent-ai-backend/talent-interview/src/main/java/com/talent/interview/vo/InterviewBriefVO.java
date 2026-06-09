package com.talent.interview.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewBriefVO {

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

    private Integer status;

    private LocalDateTime scheduledStart;
}
