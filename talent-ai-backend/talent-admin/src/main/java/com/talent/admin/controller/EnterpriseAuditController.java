package com.talent.admin.controller;

import com.talent.admin.common.PageResult;
import com.talent.admin.common.Result;
import com.talent.admin.dto.EnterpriseAuditQuery;
import com.talent.admin.dto.EnterpriseRejectRequest;
import com.talent.admin.entity.EnterpriseAudit;
import com.talent.admin.service.IEnterpriseAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业资质审核 控制器
 *
 * @author TalentAI
 */
@RestController
@RequestMapping("/api/admin/enterprises")
@RequiredArgsConstructor
public class EnterpriseAuditController {

    private final IEnterpriseAuditService enterpriseAuditService;

    /**
     * 条件分页查询
     */
    @GetMapping
    public Result<PageResult<EnterpriseAudit>> page(EnterpriseAuditQuery query) {
        return Result.success(enterpriseAuditService.pageQuery(query));
    }

    /**
     * 资质核准
     */
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        enterpriseAuditService.approve(id);
        return Result.success();
    }

    /**
     * 资质驳回
     */
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestBody @Valid EnterpriseRejectRequest request) {
        enterpriseAuditService.reject(id, request);
        return Result.success();
    }
}