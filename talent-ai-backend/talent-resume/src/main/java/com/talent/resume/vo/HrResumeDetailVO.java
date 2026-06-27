package com.talent.resume.vo;

import com.talent.resume.dto.OnlineResumeCertificateDTO;
import com.talent.resume.dto.OnlineResumeEducationDTO;
import com.talent.resume.dto.OnlineResumeProjectDTO;
import com.talent.resume.dto.OnlineResumeSkillDTO;
import com.talent.resume.dto.OnlineResumeWorkDTO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class HrResumeDetailVO {

    private Long id;

    private Long candidateId;

    private String candidateName;

    private String resumeName;

    private String summary;

    /** attachment | online */
    private String resumeType;

    private Integer isDefault;

    private Integer parseStatus;

    private Integer screenStatus;

    private Long attachmentId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

  /** 候选人档案 */
    private String phone;

    private String email;

    private String city;

    private String currentTitle;

    private Byte highestEdu;

    private Byte matchScore;

  /** 最近投递 */
    private String appliedJobTitle;

    private LocalDateTime appliedAt;

    private Long applicationId;

    private Long jobId;

  /** 在线简历内容 */
    private List<OnlineResumeEducationDTO> educations;

    private List<OnlineResumeWorkDTO> workExperiences;

    private List<OnlineResumeSkillDTO> skills;

    private List<OnlineResumeProjectDTO> projects;

    private List<OnlineResumeCertificateDTO> certificates;
}
