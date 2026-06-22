package com.talent.job.controller;

import com.talent.common.api.R;
import com.talent.job.dto.OfferApprovalRequest;
import com.talent.job.dto.OfferCreateRequest;
import com.talent.job.dto.OfferQueryRequest;
import com.talent.job.service.IOfferApprovalService;
import com.talent.job.service.IOfferService;
import com.talent.job.vo.OfferDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Offer 管理控制器
 * 提供 Offer 创建、查询、撤回、候选人接受/拒绝、审批通过/拒绝等 RESTful API
 *
 * @author TalentAI
 * @since 2026-06-14
 */
@RestController
@RequestMapping("/offer")
public class OfferController {

    @Autowired
    private IOfferService offerService;

    @Autowired
    private IOfferApprovalService offerApprovalService;

    // ==================== Offer 核心操作 ====================

    /**
     * HR 创建 Offer
     * POST /api/offer
     */
    @PostMapping
    public R<OfferDetailVO> createOffer(
            @RequestBody OfferCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.createOffer(userId, request);
    }

    /**
     * 分页查询 Offer 列表
     * GET /api/offer/list
     */
    @GetMapping("/list")
    public R<Map<String, Object>> listOffers(OfferQueryRequest request) {
        return offerService.listOffers(request);
    }

    /**
     * 查询 Offer 详情（含审批链）
     * GET /api/offer/{id}
     */
    @GetMapping("/{id}")
    public R<OfferDetailVO> getOfferDetail(@PathVariable("id") Long id) {
        return offerService.getOfferDetail(id);
    }

    /**
     * HR 撤回 Offer
     * PUT /api/offer/{id}/revoke
     */
    @PutMapping("/{id}/revoke")
    public R<Void> revokeOffer(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.revokeOffer(userId, id);
    }

    /**
     * 候选人接受 Offer
     * PUT /api/offer/{id}/accept
     */
    @PutMapping("/{id}/accept")
    public R<Void> acceptOffer(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.acceptOffer(userId, id);
    }

    /**
     * 候选人拒绝 Offer
     * PUT /api/offer/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public R<Void> rejectOffer(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.rejectOffer(userId, id);
    }

    // ==================== 审批操作 ====================

    /**
     * 审批通过
     * PUT /api/offer/approval/{id}/approve
     */
    @PutMapping("/approval/{id}/approve")
    public R<Void> approveOffer(
            @PathVariable("id") Long approvalId,
            @RequestBody(required = false) OfferApprovalRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        String comment = (request != null) ? request.getComment() : null;
        return offerApprovalService.approve(userId, approvalId, comment);
    }

    /**
     * 审批拒绝
     * PUT /api/offer/approval/{id}/reject
     */
    @PutMapping("/approval/{id}/reject")
    public R<Void> rejectApproval(
            @PathVariable("id") Long approvalId,
            @RequestBody(required = false) OfferApprovalRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        String comment = (request != null) ? request.getComment() : null;
        return offerApprovalService.reject(userId, approvalId, comment);
    }
}
