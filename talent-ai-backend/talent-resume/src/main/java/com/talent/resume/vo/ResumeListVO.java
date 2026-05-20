package com.talent.resume.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResumeListVO {

    private Long id;

    private String resumeName;

    private Integer isDefault;

    private Long attachmentId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String fileUrl;

    private LocalDateTime updatedAt;
}
