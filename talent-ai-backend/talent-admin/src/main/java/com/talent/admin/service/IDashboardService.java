package com.talent.admin.service;

import com.talent.admin.dto.dashboard.DashboardData;

/**
 * 数据大屏聚合服务
 *
 * @author TalentAI
 */
public interface IDashboardService {

    /** 聚合大屏所需全部数据 */
    DashboardData getDashboardData();
}