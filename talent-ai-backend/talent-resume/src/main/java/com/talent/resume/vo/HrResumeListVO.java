package com.talent.resume.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HrResumeListVO {

    private Long id;

    private Long candidateId;

    private String candidateName;

    private String resumeName;

    private String resumeType;

    private String phone;

    private String city;

    private String currentTitle;

    private Byte highestEdu;

    private Integer screenStatus;

    private Integer parseStatus;

    private Long attachmentId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private LocalDateTime updatedAt;
}
