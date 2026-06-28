package com.talent.analytics.feign.dto;

import lombok.Data;

/** 部门岗位统计（Feign反序列化） */
@Data
public class DepartmentJobStatDTO {
    private Long deptId;
    private String deptName;
    private Integer headcount;
    private Long activeCount;
}
