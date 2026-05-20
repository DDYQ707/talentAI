package com.talent.resume.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HrResumeDetailVO {

    private Long id;

    private Long candidateId;

    private String candidateName;

    private String resumeName;

    private String summary;

    private Integer isDefault;

    private Integer parseStatus;

    private Integer screenStatus;

    private Long attachmentId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
