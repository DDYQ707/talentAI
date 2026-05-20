package com.talent.job.dto;

import lombok.Data;

/**
 * 候选人投递简历请求
 */
@Data
public class JobApplicationSubmitRequest {

    /** 岗位 ID */
    private Long jobId;

    /** 简历 ID（逻辑外键 -> talent-resume） */
    private Long resumeId;

    /**
     * 渠道：1-BOSS 2-猎头 3-内推 4-智联 5-其他；不传默认 5
     */
    private Byte channel;
}
