package com.talent.admin.controller;

import com.talent.admin.common.PageResult;
import com.talent.admin.common.Result;
import com.talent.admin.dto.JobRiskQuery;
import com.talent.admin.dto.JobTakedownRequest;
import com.talent.admin.entity.JobRisk;
import com.talent.admin.service.IJobRiskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 职位风控 控制器
 *
 * @author TalentAI
 */
@RestController
@RequestMapping("/api/admin/jobs/risk")
@RequiredArgsConstructor
public class JobRiskController {

    private final IJobRiskService jobRiskService;

    /**
     * 分页查询被标记或待审的职位（支持 keyword、status 过滤）
     */
    @GetMapping
    public Result<PageResult<JobRisk>> page(JobRiskQuery query) {
        return Result.success(jobRiskService.pageQuery(query));
    }

    /**
     * 强制下架职位
     */
    @PutMapping("/{id}/takedown")
    public Result<Void> takedown(@PathVariable Long id,
                                 @RequestBody @Valid JobTakedownRequest request,
                                 @RequestHeader(value = "X-User-Id", required = false) Long operatorId) {
        jobRiskService.takedown(id, request, operatorId);
        return Result.success();
    }
}