package com.talent.analytics.feign.dto;

import lombok.Data;

@Data
public class OfferStatsDTO {
    private Long monthlyOfferSent;
    private Double offerAcceptRate;
}
