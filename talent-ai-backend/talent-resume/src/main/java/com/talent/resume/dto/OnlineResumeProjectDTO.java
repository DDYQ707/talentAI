package com.talent.resume.dto;

import lombok.Data;

@Data
public class OnlineResumeProjectDTO {

    private Long id;

    private String projectName;

    private String role;

    private String techStack;

    private String startDate;

    private String endDate;

    private String description;

    private String linkUrl;

    private Integer sortOrder;
}
