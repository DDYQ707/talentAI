package com.talent.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 部门招聘进度 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentProgress {
    /** 部门名称 */
    private String dept;
    /** 缺口人数（headcount总和） */
    private Integer gap;
    /** 在招岗位数 */
    private Long active;
}
