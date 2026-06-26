package com.talent.admin.dto.dashboard;

import lombok.Data;

import java.io.Serializable;

/**
 * 顶部 KPI 概览 VO
 *
 * @author TalentAI
 */
@Data
public class OverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总用户数（真，auth 库 sys_user） */
    private long totalUsers;
    /** 用户环比（假，写死） */
    private double totalUsersTrend;

    /** 入驻企业数（真，master 库 talent_enterprise_audit status=1） */
    private long totalEnterprises;
    /** 企业环比（假，写死） */
    private double totalEnterprisesTrend;

    /** 今日投递峰值（真，job 库 job_application 今日） */
    private long todayDeliveryPeak;
    /** 今日投递环比（假，写死） */
    private double todayDeliveryPeakTrend;

    /** AI 风控拦截数（真，master 库 talent_job_risk status=1） */
    private long aiRiskBlocked;
    /** 风控环比（假，写死） */
    private double aiRiskBlockedTrend;
}