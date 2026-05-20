package com.talent.resume.vo;

import lombok.Data;

@Data
public class ResumePreviewVO {

    private Long attachmentId;

    private Long resumeId;

    private String fileName;

    private String fileType;

    private String presignedUrl;

    /** 链接有效秒数 */
    private Integer expiresIn;
}
