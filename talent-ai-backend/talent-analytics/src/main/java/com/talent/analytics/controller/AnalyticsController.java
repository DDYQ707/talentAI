package com.talent.analytics.controller;

import com.talent.analytics.dto.DashboardMetrics;
import com.talent.analytics.service.DashboardService;
import com.talent.common.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/hr/dashboard")
    public R<DashboardMetrics> getHrDashboard() {
        return R.ok(dashboardService.getHrDashboard());
    }
}