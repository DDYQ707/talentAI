package com.talent.resume.dto;

import lombok.Data;

@Data
public class OnlineResumeWorkDTO {

    private Long id;

    private String companyName;

    private String jobTitle;

    private String startDate;

    private String endDate;

    private String jobDescription;

    private Integer sortOrder;
}
