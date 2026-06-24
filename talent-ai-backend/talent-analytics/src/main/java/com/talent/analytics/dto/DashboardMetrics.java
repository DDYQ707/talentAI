package com.talent.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardMetrics {
    private Long activeJobs;
    private Long totalResumes;
    private Long initialScreenPass;
    private Long ongoingInterviews;
    private Long monthlyHired;
    private Long monthlyOfferSent;
    private Double offerAcceptRate;
    private Long approxMonthlyHiredFromInterview;
    private List<String> placeholderFields;
}