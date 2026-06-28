package com.talent.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 招聘趋势单月数据点 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendPoint {
    /** 月份标签，如 "1月" */
    private String month;
    /** 当月投递量 */
    private Long applications;
    /** 当月面试完成量 */
    private Long completedInterviews;
    /** 当月Offer发放量 */
    private Long offers;
}
