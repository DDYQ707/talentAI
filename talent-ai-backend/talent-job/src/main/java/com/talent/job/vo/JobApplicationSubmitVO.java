package com.talent.job.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投递成功后的返回数据
 */
@Data
public class JobApplicationSubmitVO {

    private Long id;
    private String applicationNo;
    private Long jobId;
    private String jobTitle;
    private Long resumeId;
    private Byte currentStage;
    private Byte status;
    private LocalDateTime appliedAt;
}
