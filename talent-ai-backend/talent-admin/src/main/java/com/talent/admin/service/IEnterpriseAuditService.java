package com.talent.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.EnterpriseAuditQuery;
import com.talent.admin.dto.EnterpriseRejectRequest;
import com.talent.admin.entity.EnterpriseAudit;

/**
 * 企业资质审核 服务接口
 *
 * @author TalentAI
 */
public interface IEnterpriseAuditService extends IService<EnterpriseAudit> {

    /**
     * 条件分页查询
     */
    PageResult<EnterpriseAudit> pageQuery(EnterpriseAuditQuery query);

    /**
     * 资质核准：状态变更为 1，记录审核时间
     */
    void approve(Long id);

    /**
     * 资质驳回：状态变更为 2，记录驳回理由和审核时间
     */
    void reject(Long id, EnterpriseRejectRequest request);
}