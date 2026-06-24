package com.talent.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrendPoint {
    private String month;
    private Long deliveries;
    private Long interviews;
    private Long hires;
}