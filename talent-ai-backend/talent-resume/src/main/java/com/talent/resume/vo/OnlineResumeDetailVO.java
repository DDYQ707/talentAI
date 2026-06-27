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
public class OnlineResumeDetailVO {

    private Long id;

    private String resumeName;

    private String summary;

    private Integer isDefault;

    private Integer completeness;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<OnlineResumeEducationDTO> educations;

    private List<OnlineResumeWorkDTO> workExperiences;

    private List<OnlineResumeSkillDTO> skills;

    private List<OnlineResumeProjectDTO> projects;

    private List<OnlineResumeCertificateDTO> certificates;

    /** 候选人档案工作年限，供前端完整度区分应届/在职 */
    private java.math.BigDecimal workYears;
}
