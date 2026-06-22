package com.talent.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.common.api.R;
import com.talent.job.constant.OfferConstants;
import com.talent.job.dto.OfferCreateRequest;
import com.talent.job.dto.OfferQueryRequest;
import com.talent.job.entity.JobPost;
import com.talent.job.entity.Offer;
import com.talent.job.entity.OfferApproval;
import com.talent.job.exception.BusinessException;
import com.talent.job.mapper.OfferMapper;
import com.talent.job.service.IOfferApprovalService;
import com.talent.job.service.IOfferService;
import com.talent.job.service.IJobPostService;
import com.talent.job.vo.OfferDetailVO;
import com.talent.job.vo.OfferListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p>
 * Offer 服务实现类
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-14
 */
@Service
public class OfferServiceImpl extends ServiceImpl<OfferMapper, Offer> implements IOfferService {

    @Autowired
    private IOfferApprovalService offerApprovalService;

    @Autowired
    private IJobPostService jobPostService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<OfferDetailVO> createOffer(Long hrId, OfferCreateRequest request) {
        // 1. 参数校验
        if (request.getJobId() == null) {
            throw new BusinessException("岗位 ID 不能为空");
        }
        if (request.getCandidateId() == null) {
            throw new BusinessException("候选人 ID 不能为空");
        }

        // 2. 查询岗位信息，自动填充快照字段
        JobPost jobPost = jobPostService.getById(request.getJobId());
        if (jobPost == null) {
            throw new BusinessException("关联岗位不存在");
        }

        // 3. 构建 Offer 实体
        Offer offer = new Offer();
        offer.setOfferNo(generateOfferNo());
        offer.setApplicationId(request.getApplicationId());
        offer.setJobId(request.getJobId());
        offer.setJobTitle(jobPost.getTitle());
        offer.setCandidateId(request.getCandidateId());
        offer.setCandidateName(request.getCandidateName());
        offer.setDeptId(jobPost.getDeptId());
        offer.setDeptName(jobPost.getDeptName());
        offer.setBaseSalary(request.getBaseSalary());
        offer.setAnnualSalary(request.getAnnualSalary());
        offer.setBonus(request.getBonus());
        offer.setSalaryRemark(request.getSalaryRemark());
        offer.setPositionLevel(request.getPositionLevel());
        offer.setExpectedOnboardDate(request.getExpectedOnboardDate());
        offer.setProbationMonths(request.getProbationMonths() != null ? request.getProbationMonths() : 3);
        offer.setHrId(hrId);
        offer.setRemark(request.getRemark());

        // 4. 判断是否需要审批
        List<Long> approverIds = request.getApproverIds();
        boolean needApproval = !CollectionUtils.isEmpty(approverIds);

        if (needApproval) {
            // 有审批人 → 初始状态为"审批中"
            offer.setStatus(OfferConstants.OFFER_STATUS_APPROVING);
        } else {
            // 无审批人 → 直接进入"已通过"状态
            offer.setStatus(OfferConstants.OFFER_STATUS_APPROVED);
        }

        // 5. 保存 Offer
        this.save(offer);

        // 6. 初始化审批链
        if (needApproval) {
            List<OfferApproval> approvals = new ArrayList<>();
            for (int i = 0; i < approverIds.size(); i++) {
                OfferApproval approval = new OfferApproval();
                approval.setOfferId(offer.getId());
                approval.setSeq(i + 1);
                approval.setApproverId(approverIds.get(i));
                approval.setStatus(OfferConstants.APPROVAL_STATUS_PENDING);
                approvals.add(approval);
            }
            offerApprovalService.saveBatch(approvals);
        }

        // 7. 返回详情
        return getOfferDetail(offer.getId());
    }

    @Override
    public R<Map<String, Object>> listOffers(OfferQueryRequest request) {
        Page<Offer> pageParam = new Page<>(request.getCurrent(), request.getSize());

        LambdaQueryWrapper<Offer> queryWrapper = new LambdaQueryWrapper<>();

        // 可选过滤条件
        if (request.getStatus() != null) {
            queryWrapper.eq(Offer::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getCandidateName())) {
            queryWrapper.like(Offer::getCandidateName, request.getCandidateName());
        }
        if (StringUtils.hasText(request.getJobTitle())) {
            queryWrapper.like(Offer::getJobTitle, request.getJobTitle());
        }
        if (request.getJobId() != null) {
            queryWrapper.eq(Offer::getJobId, request.getJobId());
        }

        queryWrapper.orderByDesc(Offer::getCreatedAt);

        Page<Offer> pageResult = this.page(pageParam, queryWrapper);

        // 转换为 OfferListVO
        List<OfferListVO> voList = new ArrayList<>();
        for (Offer offer : pageResult.getRecords()) {
            OfferListVO vo = new OfferListVO();
            vo.setId(offer.getId());
            vo.setOfferNo(offer.getOfferNo());
            vo.setJobTitle(offer.getJobTitle());
            vo.setCandidateName(offer.getCandidateName());
            vo.setDeptName(offer.getDeptName());
            vo.setBaseSalary(offer.getBaseSalary());
            vo.setAnnualSalary(offer.getAnnualSalary());
            vo.setPositionLevel(offer.getPositionLevel());
            vo.setStatus(offer.getStatus());
            vo.setStatusText(OfferConstants.offerStatusText(offer.getStatus()));
            vo.setHrName(offer.getHrName());
            vo.setCreatedAt(offer.getCreatedAt());
            voList.add(vo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", pageResult.getTotal());
        data.put("current", pageResult.getCurrent());
        data.put("pages", pageResult.getPages());
        data.put("records", voList);

        return R.ok(data);
    }

    @Override
    public R<OfferDetailVO> getOfferDetail(Long offerId) {
        Offer offer = this.getById(offerId);
        if (offer == null) {
            throw new BusinessException("Offer 不存在");
        }

        // 查询审批链
        LambdaQueryWrapper<OfferApproval> approvalQuery = new LambdaQueryWrapper<>();
        approvalQuery.eq(OfferApproval::getOfferId, offerId);
        approvalQuery.orderByAsc(OfferApproval::getSeq);
        List<OfferApproval> approvals = offerApprovalService.list(approvalQuery);

        // 构建 VO
        OfferDetailVO vo = new OfferDetailVO();
        vo.setId(offer.getId());
        vo.setOfferNo(offer.getOfferNo());
        vo.setApplicationId(offer.getApplicationId());
        vo.setJobId(offer.getJobId());
        vo.setJobTitle(offer.getJobTitle());
        vo.setCandidateId(offer.getCandidateId());
        vo.setCandidateName(offer.getCandidateName());
        vo.setDeptId(offer.getDeptId());
        vo.setDeptName(offer.getDeptName());
        vo.setBaseSalary(offer.getBaseSalary());
        vo.setAnnualSalary(offer.getAnnualSalary());
        vo.setBonus(offer.getBonus());
        vo.setSalaryRemark(offer.getSalaryRemark());
        vo.setPositionLevel(offer.getPositionLevel());
        vo.setExpectedOnboardDate(offer.getExpectedOnboardDate());
        vo.setProbationMonths(offer.getProbationMonths());
        vo.setStatus(offer.getStatus());
        vo.setStatusText(OfferConstants.offerStatusText(offer.getStatus()));
        vo.setHrId(offer.getHrId());
        vo.setHrName(offer.getHrName());
        vo.setRemark(offer.getRemark());
        vo.setCreatedAt(offer.getCreatedAt());
        vo.setUpdatedAt(offer.getUpdatedAt());
        vo.setApprovals(approvals);

        return R.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> revokeOffer(Long hrId, Long offerId) {
        Offer offer = this.getById(offerId);
        if (offer == null) {
            throw new BusinessException("Offer 不存在");
        }

        // 只有特定状态下才允许撤回（待审批、审批中、已通过、已发放）
        byte currentStatus = offer.getStatus();
        if (currentStatus != OfferConstants.OFFER_STATUS_PENDING
                && currentStatus != OfferConstants.OFFER_STATUS_APPROVING
                && currentStatus != OfferConstants.OFFER_STATUS_APPROVED
                && currentStatus != OfferConstants.OFFER_STATUS_ISSUED) {
            throw new BusinessException("当前状态不允许撤回 Offer");
        }

        offer.setStatus(OfferConstants.OFFER_STATUS_REVOKED);
        this.updateById(offer);

        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> acceptOffer(Long candidateId, Long offerId) {
        Offer offer = this.getById(offerId);
        if (offer == null) {
            throw new BusinessException("Offer 不存在");
        }
        if (!offer.getCandidateId().equals(candidateId)) {
            throw new BusinessException("无权操作此 Offer");
        }

        // 只有"已发放"状态才可接受
        if (offer.getStatus() != OfferConstants.OFFER_STATUS_ISSUED) {
            throw new BusinessException("当前 Offer 状态不允许接受操作，仅[已发放]的 Offer 可以接受");
        }

        offer.setStatus(OfferConstants.OFFER_STATUS_ACCEPTED);
        this.updateById(offer);

        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> rejectOffer(Long candidateId, Long offerId) {
        Offer offer = this.getById(offerId);
        if (offer == null) {
            throw new BusinessException("Offer 不存在");
        }
        if (!offer.getCandidateId().equals(candidateId)) {
            throw new BusinessException("无权操作此 Offer");
        }

        // 只有"已发放"状态才可拒绝
        if (offer.getStatus() != OfferConstants.OFFER_STATUS_ISSUED) {
            throw new BusinessException("当前 Offer 状态不允许拒绝操作，仅[已发放]的 Offer 可以拒绝");
        }

        offer.setStatus(OfferConstants.OFFER_STATUS_DECLINED);
        this.updateById(offer);

        return R.ok();
    }

    // ==================== 私有方法 ====================

    /**
     * 生成 Offer 编号：OFR-yyyyMMdd-随机4位
     */
    private String generateOfferNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return OfferConstants.OFFER_NO_PREFIX + "-" + datePart + "-" + random;
    }
}
