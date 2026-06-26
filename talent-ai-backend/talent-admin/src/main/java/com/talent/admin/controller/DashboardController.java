package com.talent.admin.controller;

import com.talent.admin.common.Result;
import com.talent.admin.dto.dashboard.DashboardData;
import com.talent.admin.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据大屏聚合 控制器
 *
 * @author TalentAI
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    /**
     * 获取大屏聚合数据。
     * GET /api/admin/dashboard
     */
    @GetMapping
    public Result<DashboardData> dashboard() {
        return Result.success(dashboardService.getDashboardData());
    }
}