package com.talent.job.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新 Offer 薪资与入职信息 */
@Data
public class OfferUpdateRequest {

    private BigDecimal baseSalary;
    private BigDecimal annualSalary;
    private BigDecimal bonus;
    private String salaryRemark;
    private String positionLevel;
    private LocalDate expectedOnboardDate;
    private Integer probationMonths;
    private String remark;
}
