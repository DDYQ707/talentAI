package com.talent.job.service.impl;

import com.talent.job.entity.JobPost;
import com.talent.job.mapper.JobPostMapper;
import com.talent.job.service.IJobPostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 招聘岗位表（talent-job库） 服务实现类
 * </p>
 *
 * @author TalentAI
 * @since 2026-05-19
 */
@Service
public class JobPostServiceImpl extends ServiceImpl<JobPostMapper, JobPost> implements IJobPostService {

}
