package com.talent.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.EnterpriseAuditQuery;
import com.talent.admin.dto.EnterpriseRejectRequest;
import com.talent.admin.entity.EnterpriseAudit;
import com.talent.admin.exception.BusinessException;
import com.talent.admin.mapper.EnterpriseAuditMapper;
import com.talent.admin.service.IEnterpriseAuditService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 企业资质审核 服务实现
 *
 * @author TalentAI
 */
@Service
public class EnterpriseAuditServiceImpl extends ServiceImpl<EnterpriseAuditMapper, EnterpriseAudit>
        implements IEnterpriseAuditService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public PageResult<EnterpriseAudit> pageQuery(EnterpriseAuditQuery query) {
        int pageNum = (query.getPage() != null && query.getPage() > 0) ? query.getPage() : 1;
        int pageSize = (query.getSize() != null && query.getSize() > 0) ? query.getSize() : 10;

        LambdaQueryWrapper<EnterpriseAudit> wrapper = new LambdaQueryWrapper<>();

        // keyword：企业名称 或 信用代码 模糊匹配
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(EnterpriseAudit::getCompanyName, kw)
                    .or().like(EnterpriseAudit::getCreditCode, kw));
        }

        // status 精确匹配
        if (query.getStatus() != null) {
            wrapper.eq(EnterpriseAudit::getStatus, query.getStatus());
        }

        // 提交时间区间
        if (StringUtils.hasText(query.getStartDate())) {
            wrapper.ge(EnterpriseAudit::getSubmittedAt, parseStart(query.getStartDate()));
        }
        if (StringUtils.hasText(query.getEndDate())) {
            wrapper.le(EnterpriseAudit::getSubmittedAt, parseEnd(query.getEndDate()));
        }

        wrapper.orderByDesc(EnterpriseAudit::getSubmittedAt);

        Page<EnterpriseAudit> p = page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(p);
    }

    @Override
    public void approve(Long id) {
        EnterpriseAudit audit = getExisting(id);
        if (audit.getStatus() != null && audit.getStatus() == 1) {
            throw new BusinessException("该企业已通过审核，请勿重复操作");
        }
        EnterpriseAudit update = new EnterpriseAudit();
        update.setId(id);
        update.setStatus(1);
        update.setRejectReason(null);
        update.setReviewedAt(LocalDateTime.now());
        if (!updateById(update)) {
            throw new BusinessException("核准失败");
        }
    }

    @Override
    public void reject(Long id, EnterpriseRejectRequest request) {
        if (request == null || !StringUtils.hasText(request.getRejectReason())) {
            throw new BusinessException(400, "驳回理由不能为空");
        }
        getExisting(id);
        EnterpriseAudit update = new EnterpriseAudit();
        update.setId(id);
        update.setStatus(2);
        update.setRejectReason(request.getRejectReason().trim());
        update.setReviewedAt(LocalDateTime.now());
        if (!updateById(update)) {
            throw new BusinessException("驳回失败");
        }
    }

    private EnterpriseAudit getExisting(Long id) {
        if (id == null) {
            throw new BusinessException(400, "企业ID不能为空");
        }
        EnterpriseAudit audit = getById(id);
        if (audit == null) {
            throw new BusinessException(404, "企业审核记录不存在");
        }
        return audit;
    }

    private LocalDateTime parseStart(String date) {
        try {
            return LocalDate.parse(date, DATE_FMT).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "开始日期格式错误，应为 yyyy-MM-dd");
        }
    }

    private LocalDateTime parseEnd(String date) {
        try {
            return LocalDate.parse(date, DATE_FMT).atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "结束日期格式错误，应为 yyyy-MM-dd");
        }
    }
}