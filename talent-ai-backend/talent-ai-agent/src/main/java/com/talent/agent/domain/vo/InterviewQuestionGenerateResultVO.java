package com.talent.agent.domain.vo;

import java.util.List;
import lombok.Data;

@Data
public class InterviewQuestionGenerateResultVO {

    private Long interviewId;

    private Long applicationId;

    private Long jobId;

    private Long resumeId;

    private String candidateName;

    private String jobTitle;

    private List<InterviewQuestionVO> questions;
}
