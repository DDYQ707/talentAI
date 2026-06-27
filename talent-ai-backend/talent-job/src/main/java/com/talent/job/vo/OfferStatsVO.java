package com.talent.job.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OfferStatsVO {
    /** 本月进入 Offer 流程的数量（已通过审批 / 已发放 / 候选人已响应） */
    private Long monthlyOfferSent;
    /** 本月候选人 Offer 接受率（0~1），无已决 Offer 时为 0 */
    private Double offerAcceptRate;
}
