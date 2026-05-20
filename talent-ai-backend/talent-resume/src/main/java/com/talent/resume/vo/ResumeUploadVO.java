package com.talent.resume.vo;

import lombok.Data;

@Data
public class ResumeUploadVO {

    private Long resumeId;

    private Long attachmentId;

    private String resumeName;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String fileUrl;

    private String objectKey;
}
