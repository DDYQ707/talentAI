package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class JobBriefInfo {

    private Long id;
    private String title;
    private String workCity;
    private String jobDescription;
    private String jobRequirements;
    private String skillTags;
    private Integer experienceYearsMin;
    private Integer educationRequirement;
}
