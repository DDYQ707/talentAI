package com.talent.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.JobRiskQuery;
import com.talent.admin.dto.JobTakedownRequest;
import com.talent.admin.entity.JobRisk;
import com.talent.admin.exception.BusinessException;
import com.talent.admin.feign.JobAdminFeignClient;
import com.talent.admin.mapper.JobRiskMapper;
import com.talent.admin.service.IJobRiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 职位风控 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobRiskServiceImpl extends ServiceImpl<JobRiskMapper, JobRisk> implements IJobRiskService {

    private final JobAdminFeignClient jobAdminFeignClient;

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

        syncJobPostTakedown(job, request.getReason().trim());

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

    /** 同步下架 talent-job 真实岗位（Feign）；失败时记录日志但不阻断 admin 侧审计记录 */
    private void syncJobPostTakedown(JobRisk job, String reason) {
        try {
            Map<String, Object> body = new HashMap<>();
            if (job.getJobPostId() != null) {
                body.put("jobPostId", job.getJobPostId());
            }
            body.put("jobTitle", job.getJobTitle());
            body.put("publisherId", job.getPublisherId());
            body.put("reason", reason);
            Map<String, Object> resp = jobAdminFeignClient.adminTakedown(body);
            Object code = resp != null ? resp.get("code") : null;
            if (code instanceof Number n && n.intValue() != 200) {
                log.warn("[JobRisk] talent-job 同步下架未成功: {}", resp);
            }
        } catch (Exception e) {
            log.warn("[JobRisk] talent-job Feign 同步下架失败: {}", e.getMessage());
        }
    }
}