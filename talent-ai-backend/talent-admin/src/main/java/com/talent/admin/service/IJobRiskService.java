package com.talent.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.JobRiskQuery;
import com.talent.admin.dto.JobTakedownRequest;
import com.talent.admin.entity.JobRisk;

/**
 * 职位风控 服务接口
 *
 * @author TalentAI
 */
public interface IJobRiskService extends IService<JobRisk> {

    /**
     * 分页查询被标记或待审的职位
     */
    PageResult<JobRisk> pageQuery(JobRiskQuery query);

    /**
     * 强制下架：状态变更为 2，记录下架时间与操作员ID
     *
     * @param id         职位ID
     * @param request    下架请求（含原因）
     * @param operatorId 当前操作员ID
     */
    void takedown(Long id, JobTakedownRequest request, Long operatorId);
}