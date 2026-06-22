package com.talent.job.service;

import com.talent.common.api.R;
import com.talent.job.entity.OfferApproval;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * Offer 审批服务接口
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-14
 */
public interface IOfferApprovalService extends IService<OfferApproval> {

    /**
     * 审批通过
     *
     * @param approverId 审批人用户 ID
     * @param approvalId 审批节点 ID
     * @param comment    审批意见
     * @return 操作结果
     */
    R<Void> approve(Long approverId, Long approvalId, String comment);

    /**
     * 审批拒绝
     *
     * @param approverId 审批人用户 ID
     * @param approvalId 审批节点 ID
     * @param comment    审批意见
     * @return 操作结果
     */
    R<Void> reject(Long approverId, Long approvalId, String comment);
}
