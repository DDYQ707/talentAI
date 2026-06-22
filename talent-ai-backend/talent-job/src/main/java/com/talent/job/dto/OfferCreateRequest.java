package com.talent.job.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建 Offer 请求体
 */
@Data
public class OfferCreateRequest {

    /**
     * 关联投递申请 ID（可选）
     */
    private Long applicationId;

    /**
     * 关联岗位 ID（必填）
     */
    private Long jobId;

    /**
     * 候选人用户 ID（必填）
     */
    private Long candidateId;

    /**
     * 候选人姓名（必填）
     */
    private String candidateName;

    /**
     * 月度基本薪资
     */
    private BigDecimal baseSalary;

    /**
     * 年薪总包
     */
    private BigDecimal annualSalary;

    /**
     * 年终奖
     */
    private BigDecimal bonus;

    /**
     * 薪资补充说明
     */
    private String salaryRemark;

    /**
     * 职级（如 P6、M1）
     */
    private String positionLevel;

    /**
     * 预计入职日期
     */
    private LocalDate expectedOnboardDate;

    /**
     * 试用期月数
     */
    private Integer probationMonths;

    /**
     * Offer 备注
     */
    private String remark;

    /**
     * 审批人 ID 列表（按审批顺序排列）
     * 如果为空则无需审批，直接进入"已通过"状态
     */
    private List<Long> approverIds;
}
