package com.talent.admin.dto;

import lombok.Data;

/**
 * 企业资质审核分页查询参数
 *
 * @author TalentAI
 */
@Data
public class EnterpriseAuditQuery {

    /** 关键字：企业名称 或 统一社会信用代码 */
    private String keyword;

    /** 状态：0=待审核, 1=已通过, 2=已驳回 */
    private Integer status;

    /** 提交开始日期，格式 yyyy-MM-dd */
    private String startDate;

    /** 提交结束日期，格式 yyyy-MM-dd */
    private String endDate;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;
}