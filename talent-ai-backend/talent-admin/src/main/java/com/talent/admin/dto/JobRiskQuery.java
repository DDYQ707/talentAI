package com.talent.admin.dto;

import lombok.Data;

/**
 * 职位风控分页查询参数
 *
 * @author TalentAI
 */
@Data
public class JobRiskQuery {

    /** 关键字：职位名称 或 企业名称 */
    private String keyword;

    /** 状态：0=正常, 1=风险预警, 2=已下架 */
    private Integer status;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;
}