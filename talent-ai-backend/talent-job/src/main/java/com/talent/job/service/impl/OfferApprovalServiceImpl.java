package com.talent.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.common.api.R;
import com.talent.job.constant.OfferConstants;
import com.talent.job.entity.Offer;
import com.talent.job.entity.OfferApproval;
import com.talent.job.exception.BusinessException;
import com.talent.job.mapper.OfferApprovalMapper;
import com.talent.job.service.IOfferApprovalService;
import com.talent.job.service.IOfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * Offer 审批服务实现类
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-14
 */
@Service
public class OfferApprovalServiceImpl extends ServiceImpl<OfferApprovalMapper, OfferApproval>
        implements IOfferApprovalService {

    @Autowired
    @Lazy
    private IOfferService offerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> approve(Long approverId, Long approvalId, String comment) {
        // 1. 查询审批节点
        OfferApproval approval = this.getById(approvalId);
        if (approval == null) {
            throw new BusinessException("审批节点不存在");
        }

        // 2. 校验操作人
        if (!approval.getApproverId().equals(approverId)) {
            throw new BusinessException("无权操作此审批节点");
        }

        // 3. 校验审批节点状态
        if (approval.getStatus() != OfferConstants.APPROVAL_STATUS_PENDING) {
            throw new BusinessException("该审批节点已处理，不可重复操作");
        }

        // 4. 校验 Offer 状态
        Offer offer = offerService.getById(approval.getOfferId());
        if (offer == null) {
            throw new BusinessException("关联 Offer 不存在");
        }
        if (offer.getStatus() != OfferConstants.OFFER_STATUS_APPROVING) {
            throw new BusinessException("当前 Offer 状态不允许审批操作");
        }

        // 5. 校验是否轮到当前节点审批（前序节点必须全部通过）
        checkPredecessorsApproved(approval);

        // 6. 更新审批节点
        approval.setStatus(OfferConstants.APPROVAL_STATUS_APPROVED);
        approval.setComment(comment);
        approval.setApprovedAt(LocalDateTime.now());
        this.updateById(approval);

        // 7. 判断是否所有审批节点都已通过
        boolean allApproved = checkAllApproved(approval.getOfferId());
        if (allApproved) {
            // 所有审批通过 → Offer 状态变为"已通过"
            offer.setStatus(OfferConstants.OFFER_STATUS_APPROVED);
            offerService.updateById(offer);
        }

        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> reject(Long approverId, Long approvalId, String comment) {
        // 1. 查询审批节点
        OfferApproval approval = this.getById(approvalId);
        if (approval == null) {
            throw new BusinessException("审批节点不存在");
        }

        // 2. 校验操作人
        if (!approval.getApproverId().equals(approverId)) {
            throw new BusinessException("无权操作此审批节点");
        }

        // 3. 校验审批节点状态
        if (approval.getStatus() != OfferConstants.APPROVAL_STATUS_PENDING) {
            throw new BusinessException("该审批节点已处理，不可重复操作");
        }

        // 4. 校验 Offer 状态
        Offer offer = offerService.getById(approval.getOfferId());
        if (offer == null) {
            throw new BusinessException("关联 Offer 不存在");
        }
        if (offer.getStatus() != OfferConstants.OFFER_STATUS_APPROVING) {
            throw new BusinessException("当前 Offer 状态不允许审批操作");
        }

        // 5. 校验是否轮到当前节点审批
        checkPredecessorsApproved(approval);

        // 6. 更新审批节点为拒绝
        approval.setStatus(OfferConstants.APPROVAL_STATUS_REJECTED);
        approval.setComment(comment);
        approval.setApprovedAt(LocalDateTime.now());
        this.updateById(approval);

        // 7. 任一节点拒绝 → Offer 整体状态变为"已拒绝"
        offer.setStatus(OfferConstants.OFFER_STATUS_REJECTED);
        offerService.updateById(offer);

        return R.ok();
    }

    // ==================== 私有方法 ====================

    /**
     * 校验前序审批节点是否全部通过
     */
    private void checkPredecessorsApproved(OfferApproval currentApproval) {
        if (currentApproval.getSeq() <= 1) {
            // 第一个审批节点，无前序
            return;
        }

        LambdaQueryWrapper<OfferApproval> query = new LambdaQueryWrapper<>();
        query.eq(OfferApproval::getOfferId, currentApproval.getOfferId());
        query.lt(OfferApproval::getSeq, currentApproval.getSeq());
        query.ne(OfferApproval::getStatus, OfferConstants.APPROVAL_STATUS_APPROVED);

        long count = this.count(query);
        if (count > 0) {
            throw new BusinessException("前序审批节点尚未全部通过，请等待");
        }
    }

    /**
     * 检查该 Offer 的所有审批节点是否都已通过
     */
    private boolean checkAllApproved(Long offerId) {
        LambdaQueryWrapper<OfferApproval> query = new LambdaQueryWrapper<>();
        query.eq(OfferApproval::getOfferId, offerId);
        query.ne(OfferApproval::getStatus, OfferConstants.APPROVAL_STATUS_APPROVED);

        return this.count(query) == 0;
    }
}
