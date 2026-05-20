package com.talent.job.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 候选人投递列表项
 */
@Data
public class JobApplicationListVO {

    private Long id;
    private String applicationNo;
    private Long jobId;
    private String jobTitle;
    /** 投递使用的简历 ID */
    private Long resumeId;
    private String resumeName;
    private Long attachmentId;
    private String attachmentFileName;
    private String attachmentFileType;
    private Byte currentStage;
    private Byte status;
    private Byte matchScore;
    private LocalDateTime appliedAt;
}
