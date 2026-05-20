package com.talent.resume.dto;

import java.util.List;
import lombok.Data;

@Data
public class OnlineResumeSaveRequest {

    private String resumeName;

    private String summary;

    private Integer isDefault;

    private List<OnlineResumeEducationDTO> educations;

    private List<OnlineResumeWorkDTO> workExperiences;

    private List<OnlineResumeSkillDTO> skills;
}
