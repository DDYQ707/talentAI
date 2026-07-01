package com.talent.job.service;

import com.talent.common.api.R;
import com.talent.job.dto.OfferCreateRequest;
import com.talent.job.dto.OfferQueryRequest;
import com.talent.job.dto.OfferUpdateRequest;
import com.talent.job.entity.Offer;
import com.talent.job.vo.OfferDetailVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * Offer 服务接口
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-14
 */
public interface IOfferService extends IService<Offer> {

    /**
     * HR 创建 Offer（含审批链初始化）
     *
     * @param hrId    发起 HR 用户 ID
     * @param request 创建请求
     * @return Offer 详情
     */
    R<OfferDetailVO> createOffer(Long hrId, OfferCreateRequest request);

    /**
     * 分页查询 Offer 列表
     */
    R<Map<String, Object>> listOffers(OfferQueryRequest request, Long userId, String role);

    /**
     * 按投递 ID 查询最近一条 Offer（候选人仅能查本人投递）
     */
    R<OfferDetailVO> getOfferByApplication(Long applicationId, Long userId, String role);

    /**
     * HR 撤回 Offer
     *
     * @param hrId    操作 HR 用户 ID
     * @param offerId Offer ID
     * @return 操作结果
     */
    R<Void> revokeOffer(Long hrId, Long offerId);

    /**
     * 候选人接受 Offer
     *
     * @param candidateId 候选人用户 ID
     * @param offerId     Offer ID
     * @return 操作结果
     */
    R<Void> acceptOffer(Long candidateId, Long offerId);

    /**
     * 候选人拒绝 Offer
     *
     * @param candidateId 候选人用户 ID
     * @param offerId     Offer ID
     * @return 操作结果
     */
    R<Void> rejectOffer(Long candidateId, Long offerId);

    /**
     * 查询 Offer 详情（含审批链）
     */
    R<OfferDetailVO> getOfferDetail(Long offerId);

    /**
     * 查询 Offer 详情（含权限校验）
     */
    R<OfferDetailVO> getOfferDetail(Long offerId, Long userId, String role);

    /**
     * HR 更新 Offer 薪资等信息
     */
    R<OfferDetailVO> updateOffer(Long hrId, Long offerId, OfferUpdateRequest request);

    /**
     * HR 发放 Offer（已通过 → 已发放）
     */
    R<Void> issueOffer(Long hrId, Long offerId);
}
