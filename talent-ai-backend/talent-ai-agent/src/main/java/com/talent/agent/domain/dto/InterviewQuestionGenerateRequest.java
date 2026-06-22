package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class InterviewQuestionGenerateRequest {

    /** 面试 ID，优先使用 */
    private Long interviewId;

    /** 投递 ID，当未传 interviewId 时自动查找该投递下最新面试 */
    private Long applicationId;
}
