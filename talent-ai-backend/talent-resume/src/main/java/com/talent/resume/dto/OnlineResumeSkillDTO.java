package com.talent.resume.dto;

import lombok.Data;

@Data
public class OnlineResumeSkillDTO {

    private Long id;

    private String skillName;

    private Integer proficiencyLevel;

    private Integer sortOrder;
}
