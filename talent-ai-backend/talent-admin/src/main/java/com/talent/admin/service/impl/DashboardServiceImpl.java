package com.talent.admin.service.impl;

import com.talent.admin.dto.dashboard.DashboardData;
import com.talent.admin.dto.dashboard.IndustryVO;
import com.talent.admin.dto.dashboard.OverviewVO;
import com.talent.admin.dto.dashboard.ServiceHealthVO;
import com.talent.admin.dto.dashboard.TrendPointVO;
import com.talent.admin.mapper.DashboardAuthMapper;
import com.talent.admin.mapper.DashboardJobMapper;
import com.talent.admin.mapper.DashboardMasterMapper;
import com.talent.admin.service.IDashboardService;
import com.talent.admin.service.NacosHealthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 数据大屏聚合服务实现。
 * <p>
 * 各数据源独立容错：单项失败返回默认值（0 / 空），不导致整个接口 500。
 *
 * @author TalentAI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    /** 趋势天数 */
    private static final int TREND_DAYS = 30;
    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd");

    private final DashboardAuthMapper dashboardAuthMapper;
    private final DashboardMasterMapper dashboardMasterMapper;
    private final DashboardJobMapper dashboardJobMapper;
    private final NacosHealthClient nacosHealthClient;

    @Override
    public DashboardData getDashboardData() {
        DashboardData data = new DashboardData();
        data.setOverview(buildOverview());
        data.setSupplyDemandTrend(buildSupplyDemandTrend());
        data.setIndustryTalent(buildIndustryTalent());
        data.setServiceHealth(safe("serviceHealth", nacosHealthClient::probeAll, Collections.emptyList()));
        return data;
    }

    /** overview：4 个真查 + 4 个写死环比（真实值为 0 时环比强制归 0，避免“0 个却上涨”的矛盾） */
    private OverviewVO buildOverview() {
        OverviewVO vo = new OverviewVO();
        long totalUsers = safe("totalUsers", dashboardAuthMapper::countUsers, 0L);
        long totalEnterprises = safe("totalEnterprises", dashboardMasterMapper::countEnterprises, 0L);
        long aiRiskBlocked = safe("aiRiskBlocked", dashboardMasterMapper::countRiskBlocked, 0L);
        long todayDeliveryPeak = safe("todayDeliveryPeak", dashboardJobMapper::countTodayDelivery, 0L);

        vo.setTotalUsers(totalUsers);
        vo.setTotalEnterprises(totalEnterprises);
        vo.setAiRiskBlocked(aiRiskBlocked);
        vo.setTodayDeliveryPeak(todayDeliveryPeak);

        // 环比暂无历史快照数据源，返回 0 表示「暂无对比」
        vo.setTotalUsersTrend(0.0);
        vo.setTotalEnterprisesTrend(0.0);
        vo.setTodayDeliveryPeakTrend(0.0);
        vo.setAiRiskBlockedTrend(0.0);
        return vo;
    }

    /** 真实值为 0 时环比归 0（保留供后续接入历史快照） */
    private double trendFor(long actualValue, double presetTrend) {
        return actualValue == 0L ? 0.0 : presetTrend;
    }

    /** supplyDemandTrend：近30天，补 0，MM-dd 升序 */
    private List<TrendPointVO> buildSupplyDemandTrend() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(TREND_DAYS - 1L);
        LocalDateTime start = startDate.atStartOfDay();

        Map<String, Long> deliveryMap = toDayCountMap(
                safe("deliveriesByDay", () -> dashboardJobMapper.deliveriesByDay(start), Collections.emptyList()));
        Map<String, Long> publishMap = toDayCountMap(
                safe("publicationsByDay", () -> dashboardJobMapper.publicationsByDay(start), Collections.emptyList()));

        List<TrendPointVO> list = new ArrayList<>(TREND_DAYS);
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate d = startDate.plusDays(i);
            String key = d.toString(); // yyyy-MM-dd
            list.add(new TrendPointVO(
                    d.format(MD),
                    deliveryMap.getOrDefault(key, 0L),
                    publishMap.getOrDefault(key, 0L)));
        }
        return list;
    }

    /** industryTalent：按 work_city 真查，失败/为空则 mock 降级 */
    private List<IndustryVO> buildIndustryTalent() {
        List<Map<String, Object>> rows = safe("industryByCity",
                dashboardJobMapper::industryByCity, Collections.emptyList());
        List<IndustryVO> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object industry = row.get("industry");
            list.add(new IndustryVO(
                    industry == null ? "其他" : industry.toString(),
                    toLong(row.get("value"))));
        }
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        return list;
    }

    /** 将 [{d,c}] 行转为 {yyyy-MM-dd -> count} */
    private Map<String, Long> toDayCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object d = row.get("d");
            if (d == null) {
                continue;
            }
            // d 可能是 java.sql.Date / LocalDate / String，统一取前 10 位 yyyy-MM-dd
            String key = d.toString();
            if (key.length() >= 10) {
                key = key.substring(0, 10);
            }
            map.put(key, toLong(row.get("c")));
        }
        return map;
    }

    private long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 通用容错包装：异常时返回默认值并记日志 */
    private <T> T safe(String tag, Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("[Dashboard] 指标 {} 查询失败，使用默认值: {}", tag, e.getMessage());
            return fallback;
        }
    }
}