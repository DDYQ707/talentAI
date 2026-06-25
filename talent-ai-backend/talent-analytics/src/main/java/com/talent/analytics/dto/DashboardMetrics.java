package com.talent.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardMetrics {
    private Long activeJobs;
    private Long totalResumes;
    private Long monthlyApplications;
    private Long initialScreenPass;
    private Long ongoingInterviews;
    private Long completedInterviewsThisMonth;
    private Long monthlyHired;
    private Long monthlyOfferSent;
    private Double offerAcceptRate;
    private Long approxMonthlyHiredFromInterview;
    private List<FunnelStage> funnel;
    private List<String> placeholderFields;
}