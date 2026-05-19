package com.talent.job.service.impl;

import com.talent.job.entity.JobApplication;
import com.talent.job.mapper.JobApplicationMapper;
import com.talent.job.service.IJobApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 投递申请表（talent-job库） 服务实现类
 * </p>
 *
 * @author TalentAI
 * @since 2026-05-19
 */
@Service
public class JobApplicationServiceImpl extends ServiceImpl<JobApplicationMapper, JobApplication> implements IJobApplicationService {

}
