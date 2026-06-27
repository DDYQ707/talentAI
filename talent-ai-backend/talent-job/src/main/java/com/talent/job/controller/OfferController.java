package com.talent.job.controller;

import com.talent.common.api.R;
import com.talent.job.dto.OfferApprovalRequest;
import com.talent.job.dto.OfferCreateRequest;
import com.talent.job.dto.OfferQueryRequest;
import com.talent.job.dto.OfferUpdateRequest;
import com.talent.job.service.IOfferApprovalService;
import com.talent.job.service.IOfferService;
import com.talent.job.vo.OfferDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Offer 管理控制器
 */
@RestController
@RequestMapping("/offer")
public class OfferController {

    @Autowired
    private IOfferService offerService;

    @Autowired
    private IOfferApprovalService offerApprovalService;

    @PostMapping
    public R<OfferDetailVO> createOffer(
            @RequestBody OfferCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.createOffer(userId, request);
    }

    @GetMapping("/list")
    public R<Map<String, Object>> listOffers(
            OfferQueryRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return offerService.listOffers(request, userId, role);
    }

    @GetMapping("/by-application/{applicationId}")
    public R<OfferDetailVO> getByApplication(
            @PathVariable("applicationId") Long applicationId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return offerService.getOfferByApplication(applicationId, userId, role);
    }

    @GetMapping("/{id}")
    public R<OfferDetailVO> getOfferDetail(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return offerService.getOfferDetail(id, userId, role);
    }

    @PutMapping("/{id}")
    public R<OfferDetailVO> updateOffer(
            @PathVariable("id") Long id,
            @RequestBody OfferUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.updateOffer(userId, id, request);
    }

    @PutMapping("/{id}/issue")
    public R<Void> issueOffer(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.issueOffer(userId, id);
    }

    @PutMapping("/{id}/revoke")
    public R<Void> revokeOffer(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.revokeOffer(userId, id);
    }

    @PutMapping("/{id}/accept")
    public R<Void> acceptOffer(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.acceptOffer(userId, id);
    }

    @PutMapping("/{id}/reject")
    public R<Void> rejectOffer(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未检测到登录用户信息");
        }
        return offerService.rejectOffer(userId, id);
    }

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
