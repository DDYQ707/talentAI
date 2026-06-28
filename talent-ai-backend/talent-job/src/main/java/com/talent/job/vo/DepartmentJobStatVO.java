package com.talent.job.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 部门岗位统计数据 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentJobStatVO {
    private Long deptId;
    private String deptName;
    /** 缺口 = 该部门所有岗位的 headcount 总和 */
    private Integer headcount;
    /** 在招 = 该部门下 status=招聘中 的岗位数 */
    private Long activeCount;
}
