package com.talent.job.dto;

import lombok.Data;

/**
 * HR 更新岗位请求
 */
@Data
public class JobPostUpdateRequest {

    private String title;
    private String deptName;
    private String workCity;
    private Byte employmentType;
    private Integer salaryMin;
    private Integer salaryMax;
    private Boolean salaryNegotiable;
    private Integer headcount;
    private Byte priority;
    private Byte status;
    private String jobDescription;
    private String jobRequirements;
    private String skillTags;
}
