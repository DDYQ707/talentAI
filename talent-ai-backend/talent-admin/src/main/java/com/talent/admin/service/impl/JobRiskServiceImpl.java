package com.talent.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.JobRiskQuery;
import com.talent.admin.dto.JobTakedownRequest;
import com.talent.admin.entity.JobRisk;
import com.talent.admin.exception.BusinessException;
import com.talent.admin.mapper.JobRiskMapper;
import com.talent.admin.service.IJobRiskService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 职位风控 服务实现
 *
 * @author TalentAI
 */
@Service
public class JobRiskServiceImpl extends ServiceImpl<JobRiskMapper, JobRisk> implements IJobRiskService {

    @Override
    public PageResult<JobRisk> pageQuery(JobRiskQuery query) {
        int pageNum = (query.getPage() != null && query.getPage() > 0) ? query.getPage() : 1;
        int pageSize = (query.getSize() != null && query.getSize() > 0) ? query.getSize() : 10;

        LambdaQueryWrapper<JobRisk> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(JobRisk::getJobTitle, kw)
                    .or().like(JobRisk::getCompanyName, kw));
        }

        if (query.getStatus() != null) {
            wrapper.eq(JobRisk::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(JobRisk::getCreatedAt);

        Page<JobRisk> p = page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(p);
    }

    @Override
    public void takedown(Long id, JobTakedownRequest request, Long operatorId) {
        if (id == null) {
            throw new BusinessException(400, "职位ID不能为空");
        }
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new BusinessException(400, "下架原因不能为空");
        }
        JobRisk job = getById(id);
        if (job == null) {
            throw new BusinessException(404, "职位记录不存在");
        }
        if (job.getStatus() != null && job.getStatus() == 2) {
            throw new BusinessException("该职位已下架，请勿重复操作");
        }

        JobRisk update = new JobRisk();
        update.setId(id);
        update.setStatus(2);
        // 将下架原因追加到风险高危词字段，留存审计信息
        update.setRiskKeywords(request.getReason().trim());
        update.setTakenDownAt(LocalDateTime.now());
        update.setTakenDownBy(operatorId);

        if (!updateById(update)) {
            throw new BusinessException("下架失败");
        }
    }
}