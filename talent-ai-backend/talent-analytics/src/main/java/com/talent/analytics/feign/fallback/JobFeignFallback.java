package com.talent.analytics.feign.fallback;

import com.talent.analytics.feign.JobFeignClient;
import com.talent.analytics.feign.dto.DepartmentJobStatDTO;
import com.talent.analytics.feign.dto.MonthlyCountDTO;
import com.talent.analytics.feign.dto.OfferStatsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * talent-job Feign 熔断降级：返回安全默认值，与 DashboardService.safeCall 行为一致。
 */
@Slf4j
@Component
public class JobFeignFallback implements JobFeignClient {

    @Override
    public Long countActiveJobs() {
        log.warn("[Sentinel] JobFeign.countActiveJobs 降级");
        return 0L;
    }

    @Override
    public Long countMonthlyApplications() {
        log.warn("[Sentinel] JobFeign.countMonthlyApplications 降级");
        return 0L;
    }

    @Override
    public Map<Integer, Long> countApplicationsByStage(List<Integer> stages) {
        log.warn("[Sentinel] JobFeign.countApplicationsByStage 降级");
        return Collections.emptyMap();
    }

    @Override
    public Map<Integer, Long> countApplicationsByStatus(List<Integer> statuses) {
        log.warn("[Sentinel] JobFeign.countApplicationsByStatus 降级");
        return Collections.emptyMap();
    }

    @Override
    public OfferStatsDTO getOfferMetrics() {
        log.warn("[Sentinel] JobFeign.getOfferMetrics 降级");
        return null;
    }

    @Override
    public List<MonthlyCountDTO> getMonthlyApplications(int months) {
        log.warn("[Sentinel] JobFeign.getMonthlyApplications 降级");
        return Collections.emptyList();
    }

    @Override
    public List<MonthlyCountDTO> getMonthlyOffers(int months) {
        log.warn("[Sentinel] JobFeign.getMonthlyOffers 降级");
        return Collections.emptyList();
    }

    @Override
    public List<DepartmentJobStatDTO> getDepartmentJobStats() {
        log.warn("[Sentinel] JobFeign.getDepartmentJobStats 降级");
        return Collections.emptyList();
    }
}
