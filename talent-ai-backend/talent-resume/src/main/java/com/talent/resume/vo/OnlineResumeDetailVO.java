package com.talent.resume.vo;

import com.talent.resume.dto.OnlineResumeEducationDTO;
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
}
