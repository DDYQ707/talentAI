package com.talent.analytics.service;

import com.talent.analytics.dto.DashboardMetrics;
import com.talent.analytics.feign.InterviewFeignClient;
import com.talent.analytics.feign.JobFeignClient;
import com.talent.analytics.feign.ResumeFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
public class DashboardService {

    @Autowired
    private JobFeignClient jobFeign;
    @Autowired
    private ResumeFeignClient resumeFeign;
    @Autowired
    private InterviewFeignClient interviewFeign;

    public DashboardMetrics getHrDashboard() {
        String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        Long activeJobs = safeCall(() -> jobFeign.countActiveJobs(), 0L);
        Long totalResumes = safeCall(() -> resumeFeign.getTotalResumeCount(), 0L);

        Long initialScreenPass = safeCall(() -> {
            Map<Integer, Long> map = jobFeign.countApplicationsByStage(List.of(2));
            return map.getOrDefault(2, 0L);
        }, 0L);

        Long ongoingInterviews = safeCall(() -> interviewFeign.countOngoingInterviews(), 0L);

        Long monthlyHired = safeCall(() -> {
            Map<Integer, Long> map = jobFeign.countApplicationsByStatus(List.of(2));
            return map.getOrDefault(2, 0L);
        }, 0L);

        Long monthlyOfferSent = 0L;      // 占位，待 Offer 模块
        Double offerAcceptRate = 0.0;    // 占位

        Long approxHired = safeCall(() -> interviewFeign.countPassedInterviews(currentYearMonth), 0L);

        List<String> placeholders = Arrays.asList("monthlyOfferSent", "offerAcceptRate");

        return DashboardMetrics.builder()
                .activeJobs(activeJobs)
                .totalResumes(totalResumes)
                .initialScreenPass(initialScreenPass)
                .ongoingInterviews(ongoingInterviews)
                .monthlyHired(monthlyHired)
                .monthlyOfferSent(monthlyOfferSent)
                .offerAcceptRate(offerAcceptRate)
                .approxMonthlyHiredFromInterview(approxHired)
                .placeholderFields(placeholders)
                .build();
    }

    private <T> T safeCall(Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Feign call failed: {}", e.getMessage());
            return defaultValue;
        }
    }
}