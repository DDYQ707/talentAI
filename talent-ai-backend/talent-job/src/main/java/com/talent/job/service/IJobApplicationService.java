package com.talent.job.service;

import com.talent.job.dto.JobApplicationSubmitRequest;
import com.talent.job.entity.JobApplication;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 投递申请表（talent-job库） 服务类
 * </p>
 *
 * @author TalentAI
 * @since 2026-05-19
 */
public interface IJobApplicationService extends IService<JobApplication> {

    /**
     * 候选人投递简历（创建投递记录、阶段日志，并递增岗位投递数）
     */
    Map<String, Object> submitApplication(Long candidateId, String userRole, JobApplicationSubmitRequest request);

    /**
     * 查询当前候选人的投递记录（分页）
     */
    Map<String, Object> listMyApplications(Long candidateId, String userRole, Integer current, Integer size);
}
