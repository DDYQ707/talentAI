package com.talent.interview.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewScheduleResultVO {

    private Long interviewId;

    private Integer status;

    private String statusLabel;

    private String candidateName;

    private String jobTitle;

    private String interviewerName;

    private LocalDateTime scheduledStart;
}
