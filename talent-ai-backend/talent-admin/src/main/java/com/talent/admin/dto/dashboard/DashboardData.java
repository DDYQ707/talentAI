package com.talent.admin.dto.dashboard;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 数据大屏聚合 VO
 *
 * @author TalentAI
 */
@Data
public class DashboardData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 顶部 4 个 KPI 卡 */
    private OverviewVO overview;

    /** 近30日供需趋势 */
    private List<TrendPointVO> supplyDemandTrend;

    /** 行业人才分布 */
    private List<IndustryVO> industryTalent;

    /** 微服务健康 */
    private List<ServiceHealthVO> serviceHealth;
}