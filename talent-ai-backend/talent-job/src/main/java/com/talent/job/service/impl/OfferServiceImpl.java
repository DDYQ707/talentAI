package com.talent.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.common.api.R;
import com.talent.job.constant.JobApplicationConstants;
import com.talent.job.constant.OfferConstants;
import com.talent.job.dto.OfferCreateRequest;
import com.talent.job.dto.OfferQueryRequest;
import com.talent.job.dto.OfferUpdateRequest;
import com.talent.job.entity.JobApplication;
import com.talent.job.entity.JobPost;
import com.talent.job.entity.Offer;
import com.talent.job.entity.OfferApproval;
import com.talent.job.exception.BusinessException;
import com.talent.job.mapper.JobApplicationMapper;
import com.talent.job.mapper.OfferMapper;
import com.talent.job.service.IOfferApprovalService;
import com.talent.job.service.IOfferService;
import com.talent.job.service.IJobPostService;
import com.talent.job.service.OfferLifecycleHookService;
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

    @Autowired
    private JobApplicationMapper jobApplicationMapper;

    @Autowired
    private OfferLifecycleHookService offerLifecycleHookService;

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
    public R<Map<String, Object>> listOffers(OfferQueryRequest request, Long userId, String role) {
        Page<Offer> pageParam = new Page<>(request.getCurrent(), request.getSize());

        LambdaQueryWrapper<Offer> queryWrapper = new LambdaQueryWrapper<>();

        if (JobApplicationConstants.ROLE_CANDIDATE.equals(role)) {
            if (userId == null) {
                throw new BusinessException("未检测到登录用户信息");
            }
            queryWrapper.eq(Offer::getCandidateId, userId);
        }

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
        if (request.getApplicationId() != null) {
            queryWrapper.eq(Offer::getApplicationId, request.getApplicationId());
        }

        queryWrapper.orderByDesc(Offer::getCreatedAt);

        Page<Offer> pageResult = this.page(pageParam, queryWrapper);

        // 转换为 OfferListVO
        List<OfferListVO> voList = new ArrayList<>();
        for (Offer offer : pageResult.getRecords()) {
            OfferListVO vo = new OfferListVO();
            vo.setId(offer.getId());
            vo.setOfferNo(offer.getOfferNo());
            vo.setApplicationId(offer.getApplicationId());
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
        return R.ok(buildOfferDetail(offerId));
    }

    @Override
    public R<OfferDetailVO> getOfferDetail(Long offerId, Long userId, String role) {
        Offer offer = this.getById(offerId);
        if (offer == null) {
            throw new BusinessException("Offer 不存在");
        }
        assertCandidateOfferAccess(offer, userId, role);
        return getOfferDetail(offerId);
    }

    private OfferDetailVO buildOfferDetail(Long offerId) {
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

        return vo;
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
        offerLifecycleHookService.onOfferAccepted(offer, candidateId);

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
        offerLifecycleHookService.onOfferDeclined(offer, candidateId);

        return R.ok();
    }

    @Override
    public R<OfferDetailVO> getOfferByApplication(Long applicationId, Long userId, String role) {
        if (applicationId == null || applicationId <= 0) {
            throw new BusinessException("applicationId 无效");
        }
        if (JobApplicationConstants.ROLE_CANDIDATE.equals(role)) {
            assertApplicationOwnedByCandidate(applicationId, userId);
        }
        Offer offer = this.getOne(
                new LambdaQueryWrapper<Offer>()
                        .eq(Offer::getApplicationId, applicationId)
                        .orderByDesc(Offer::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        if (offer == null) {
            return R.ok(null);
        }
        if (JobApplicationConstants.ROLE_CANDIDATE.equals(role)) {
            assertCandidateOfferAccess(offer, userId, role);
        }
        return getOfferDetail(offer.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<OfferDetailVO> updateOffer(Long hrId, Long offerId, OfferUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("请求体不能为空");
        }
        Offer offer = this.getById(offerId);
        if (offer == null) {
            throw new BusinessException("Offer 不存在");
        }
        byte currentStatus = offer.getStatus();
        if (currentStatus == OfferConstants.OFFER_STATUS_REVOKED
                || currentStatus == OfferConstants.OFFER_STATUS_ACCEPTED
                || currentStatus == OfferConstants.OFFER_STATUS_DECLINED) {
            throw new BusinessException("当前状态不允许修改 Offer");
        }
        if (request.getBaseSalary() != null) {
            offer.setBaseSalary(request.getBaseSalary());
        }
        if (request.getAnnualSalary() != null) {
            offer.setAnnualSalary(request.getAnnualSalary());
        }
        if (request.getBonus() != null) {
            offer.setBonus(request.getBonus());
        }
        if (request.getSalaryRemark() != null) {
            offer.setSalaryRemark(request.getSalaryRemark());
        }
        if (request.getPositionLevel() != null) {
            offer.setPositionLevel(request.getPositionLevel());
        }
        if (request.getExpectedOnboardDate() != null) {
            offer.setExpectedOnboardDate(request.getExpectedOnboardDate());
        }
        if (request.getProbationMonths() != null) {
            offer.setProbationMonths(request.getProbationMonths());
        }
        if (request.getRemark() != null) {
            offer.setRemark(request.getRemark());
        }
        if (hrId != null && hrId > 0) {
            offer.setHrId(hrId);
        }
        this.updateById(offer);
        return getOfferDetail(offerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> issueOffer(Long hrId, Long offerId) {
        Offer offer = this.getById(offerId);
        if (offer == null) {
            throw new BusinessException("Offer 不存在");
        }
        if (offer.getStatus() != OfferConstants.OFFER_STATUS_APPROVED) {
            throw new BusinessException("仅「已通过」状态的 Offer 可发放");
        }
        offer.setStatus(OfferConstants.OFFER_STATUS_ISSUED);
        if (hrId != null && hrId > 0) {
            offer.setHrId(hrId);
        }
        this.updateById(offer);
        offerLifecycleHookService.onOfferIssued(offer);
        return R.ok();
    }

    private void assertApplicationOwnedByCandidate(Long applicationId, Long userId) {
        if (userId == null) {
            throw new BusinessException("未检测到登录用户信息");
        }
        JobApplication app = jobApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException("投递记录不存在");
        }
        if (!userId.equals(app.getCandidateId())) {
            throw new BusinessException("无权查看此 Offer");
        }
    }

    private void assertCandidateOfferAccess(Offer offer, Long userId, String role) {
        if (!JobApplicationConstants.ROLE_CANDIDATE.equals(role)) {
            return;
        }
        if (userId == null) {
            throw new BusinessException("未检测到登录用户信息");
        }
        if (offer.getCandidateId() == null || !offer.getCandidateId().equals(userId)) {
            throw new BusinessException("无权操作此 Offer");
        }
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
