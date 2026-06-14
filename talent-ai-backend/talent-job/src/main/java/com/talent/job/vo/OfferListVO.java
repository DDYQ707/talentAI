package com.talent.job.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Offer 列表视图对象（精简展示）
 */
@Data
public class OfferListVO {

    private Long id;

    private String offerNo;

    private String jobTitle;

    private String candidateName;

    private String deptName;

    private BigDecimal baseSalary;

    private BigDecimal annualSalary;

    private String positionLevel;

    /**
     * 状态码
     */
    private Byte status;

    /**
     * 状态描述
     */
    private String statusText;

    private String hrName;

    private LocalDateTime createdAt;
}
