package com.talent.resume.dto;

import lombok.Data;

@Data
public class OnlineResumeWorkDTO {

    private Long id;

    private String companyName;

    private String jobTitle;

    /** 1-全职 2-实习 3-兼职 */
    private Integer experienceType;

    private String startDate;

    private String endDate;

    private String jobDescription;

    private Integer sortOrder;
}
