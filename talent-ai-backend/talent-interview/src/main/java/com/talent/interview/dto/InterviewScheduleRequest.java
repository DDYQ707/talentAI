package com.talent.interview.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewScheduleRequest {

    private Long applicationId;

    private Long interviewerId;

    /** 1-AI初筛 2-业务初试 3-业务复试 4-HR面 5-终面 6-交叉面 7-作品评审 */
    private Integer roundType;

    private Integer roundNo;

    /** 1-视频 2-现场 3-线上评审 */
    private Integer interviewMode;

    private LocalDateTime scheduledStart;

    private LocalDateTime scheduledEnd;

    private String meetingUrl;

    private String location;
}
