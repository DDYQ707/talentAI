package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class MatchRequest {

    private Long applicationId;

    private Long jobId;

    private Long resumeId;

    private Long modelId;

    /** 关联的解析任务 ID（内部异步匹配时使用） */
    private Long parseTaskId;
}
