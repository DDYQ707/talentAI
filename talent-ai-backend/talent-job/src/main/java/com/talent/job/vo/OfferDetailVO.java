package com.talent.job.vo;

import com.talent.job.entity.OfferApproval;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Offer 详情视图对象（含审批链）
 */
@Data
public class OfferDetailVO {

    private Long id;

    private String offerNo;

    private Long applicationId;

    private Long jobId;

    private String jobTitle;

    private Long candidateId;

    private String candidateName;

    private Long deptId;

    private String deptName;

    private BigDecimal baseSalary;

    private BigDecimal annualSalary;

    private BigDecimal bonus;

    private String salaryRemark;

    private String positionLevel;

    private LocalDate expectedOnboardDate;

    private Integer probationMonths;

    /**
     * 状态码
     */
    private Byte status;

    /**
     * 状态描述（如"待审批"、"已发放"等）
     */
    private String statusText;

    private Long hrId;

    private String hrName;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 审批链节点列表（按 seq 升序）
     */
    private List<OfferApproval> approvals;
}
