package com.talent.analytics.controller;

import com.talent.analytics.dto.DashboardMetrics;
import com.talent.analytics.dto.DepartmentProgress;
import com.talent.analytics.dto.TrendPoint;
import com.talent.analytics.service.DashboardService;
import com.talent.common.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/hr/dashboard")
    public R<DashboardMetrics> getHrDashboard() {
        return R.ok(dashboardService.getHrDashboard());
    }

    @GetMapping("/hr/workbench")
    public R<DashboardMetrics> getHrWorkbench() {
        return R.ok(dashboardService.getHrDashboard());
    }

    @GetMapping("/hr/trend")
    public R<List<TrendPoint>> getRecruitmentTrend() {
        return R.ok(dashboardService.getRecruitmentTrend());
    }

    @GetMapping("/hr/department-progress")
    public R<List<DepartmentProgress>> getDepartmentProgress() {
        return R.ok(dashboardService.getDepartmentProgress());
    }
}
