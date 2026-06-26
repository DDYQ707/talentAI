package com.talent.agent.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewNoteSaveRequest {

    private Long interviewId;

    private String noteContent;
}
