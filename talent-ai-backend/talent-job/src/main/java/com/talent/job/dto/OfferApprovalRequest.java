package com.talent.job.dto;

import lombok.Data;

/**
 * 审批操作请求体
 */
@Data
public class OfferApprovalRequest {

    /**
     * 审批意见
     */
    private String comment;
}
