package com.talent.analytics.service;

import com.talent.analytics.dto.DashboardMetrics;
import com.talent.analytics.dto.FunnelStage;
import com.talent.analytics.feign.InterviewFeignClient;
import com.talent.analytics.feign.JobFeignClient;
import com.talent.analytics.feign.ResumeFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.talent.analytics.feign.dto.OfferStatsDTO;

import java.util.ArrayList;
import java.util.Collections;
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
        Long monthlyApplications = safeCall(() -> jobFeign.countMonthlyApplications(), 0L);

        Long initialScreenPass = safeCall(() -> {
            Map<Integer, Long> map = jobFeign.countApplicationsByStage(List.of(2));
            return map.getOrDefault(2, 0L);
        }, 0L);

        Long ongoingInterviews = safeCall(() -> interviewFeign.countOngoingInterviews(), 0L);
        Long completedInterviewsThisMonth = safeCall(() -> interviewFeign.countCompletedThisMonth(), 0L);

        Long monthlyHired = safeCall(() -> {
            Map<Integer, Long> map = jobFeign.countApplicationsByStatus(List.of(2));
            return map.getOrDefault(2, 0L);
        }, 0L);

        OfferStatsDTO offerStats = safeCall(jobFeign::getOfferMetrics, null);
        Long monthlyOfferSent = offerStats != null && offerStats.getMonthlyOfferSent() != null
                ? offerStats.getMonthlyOfferSent()
                : 0L;
        Double offerAcceptRate = offerStats != null && offerStats.getOfferAcceptRate() != null
                ? offerStats.getOfferAcceptRate()
                : 0.0;

        Long approxHired = safeCall(() -> interviewFeign.countPassedInterviews(currentYearMonth), 0L);

        List<String> placeholders = offerStats == null
                ? List.of("monthlyOfferSent", "offerAcceptRate")
                : Collections.emptyList();
        List<FunnelStage> funnel = buildFunnel(monthlyApplications, initialScreenPass, ongoingInterviews, monthlyOfferSent, monthlyHired);

        return DashboardMetrics.builder()
                .activeJobs(activeJobs)
                .totalResumes(totalResumes)
                .monthlyApplications(monthlyApplications)
                .initialScreenPass(initialScreenPass)
                .ongoingInterviews(ongoingInterviews)
                .completedInterviewsThisMonth(completedInterviewsThisMonth)
                .monthlyHired(monthlyHired)
                .monthlyOfferSent(monthlyOfferSent)
                .offerAcceptRate(offerAcceptRate)
                .approxMonthlyHiredFromInterview(approxHired)
                .funnel(funnel)
                .placeholderFields(placeholders)
                .build();
    }

    private List<FunnelStage> buildFunnel(
            Long monthlyApplications,
            Long initialScreenPass,
            Long ongoingInterviews,
            Long monthlyOfferSent,
            Long monthlyHired) {
        List<FunnelStage> funnel = new ArrayList<>();
        funnel.add(FunnelStage.builder().stageName("简历投递").count(monthlyApplications).build());
        funnel.add(FunnelStage.builder().stageName("AI初筛通过").count(initialScreenPass).build());
        funnel.add(FunnelStage.builder().stageName("面试进行中").count(ongoingInterviews).build());
        funnel.add(FunnelStage.builder().stageName("Offer发放").count(monthlyOfferSent).build());
        funnel.add(FunnelStage.builder().stageName("录用").count(monthlyHired).build());
        return funnel;
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