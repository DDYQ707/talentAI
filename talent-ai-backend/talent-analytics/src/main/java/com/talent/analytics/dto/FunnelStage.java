package com.talent.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FunnelStage {
    private String stageName;
    private Long count;
}