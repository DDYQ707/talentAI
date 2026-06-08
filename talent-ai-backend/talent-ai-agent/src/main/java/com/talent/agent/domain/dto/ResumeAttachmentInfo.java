package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class ResumeAttachmentInfo {

    private Long attachmentId;

    private Long resumeId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String bucketName;

    private String objectKey;
}
