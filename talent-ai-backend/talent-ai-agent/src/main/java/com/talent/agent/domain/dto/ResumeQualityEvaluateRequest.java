package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class ResumeQualityEvaluateRequest {

    private Long resumeId;

    /** 为 true 时忽略同解析任务的缓存并重新评分 */
    private Boolean forceRefresh;
}
