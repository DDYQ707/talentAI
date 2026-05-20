package com.talent.resume.dto;

import lombok.Data;

@Data
public class OnlineResumeEducationDTO {

    private Long id;

    private String schoolName;

    private String major;

    private Integer degree;

    private String startDate;

    private String endDate;

    private String description;

    private Integer sortOrder;
}
