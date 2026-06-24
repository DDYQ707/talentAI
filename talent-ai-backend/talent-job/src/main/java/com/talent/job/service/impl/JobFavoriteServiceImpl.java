package com.talent.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.job.constant.JobApplicationConstants;
import com.talent.job.entity.JobFavorite;
import com.talent.job.entity.JobPost;
import com.talent.job.mapper.JobFavoriteMapper;
import com.talent.job.service.IJobFavoriteService;
import com.talent.job.service.IJobPostService;
import com.talent.job.vo.JobFavoriteVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobFavoriteServiceImpl extends ServiceImpl<JobFavoriteMapper, JobFavorite> implements IJobFavoriteService {

    @Autowired
    private IJobPostService jobPostService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleFavorite(Long candidateId, String userRole, Long jobId) {
        Map<String, Object> result = new HashMap<>();

        if (candidateId == null) {
            result.put("code", 401);
            result.put("msg", "未检测到登录用户信息");
            return result;
        }
        if (!JobApplicationConstants.ROLE_CANDIDATE.equals(userRole)) {
            result.put("code", 403);
            result.put("msg", "仅候选人可收藏岗位");
            return result;
        }
        if (jobId == null || jobId <= 0) {
            result.put("code", 400);
            result.put("msg", "岗位 ID 不能为空");
            return result;
        }

        JobPost jobPost = jobPostService.getById(jobId);
        if (jobPost == null) {
            result.put("code", 404);
            result.put("msg", "岗位不存在");
            return result;
        }

        JobFavorite existing = getOne(new LambdaQueryWrapper<JobFavorite>()
                .eq(JobFavorite::getCandidateId, candidateId)
                .eq(JobFavorite::getJobId, jobId)
                .last("LIMIT 1"));

        Map<String, Object> data = new HashMap<>();
        if (existing != null) {
            removeById(existing.getId());
            data.put("favorited", false);
            result.put("code", 200);
            result.put("msg", "已取消收藏");
        } else {
            JobFavorite favorite = new JobFavorite();
            favorite.setCandidateId(candidateId);
            favorite.setJobId(jobId);
            favorite.setCreatedAt(LocalDateTime.now());
            save(favorite);
            data.put("favorited", true);
            result.put("code", 200);
            result.put("msg", "收藏成功");
        }
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> listFavoriteJobIds(Long candidateId, String userRole) {
        Map<String, Object> result = new HashMap<>();

        if (candidateId == null) {
            result.put("code", 401);
            result.put("msg", "未检测到登录用户信息");
            return result;
        }
        if (!JobApplicationConstants.ROLE_CANDIDATE.equals(userRole)) {
            result.put("code", 403);
            result.put("msg", "仅候选人可查询收藏");
            return result;
        }

        List<JobFavorite> favorites = list(new LambdaQueryWrapper<JobFavorite>()
                .eq(JobFavorite::getCandidateId, candidateId)
                .select(JobFavorite::getJobId));

        List<Long> jobIds = favorites.stream()
                .map(JobFavorite::getJobId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("jobIds", jobIds);
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> listMyFavorites(Long candidateId, String userRole, Integer current, Integer size) {
        Map<String, Object> result = new HashMap<>();

        if (candidateId == null) {
            result.put("code", 401);
            result.put("msg", "未检测到登录用户信息");
            return result;
        }
        if (!JobApplicationConstants.ROLE_CANDIDATE.equals(userRole)) {
            result.put("code", 403);
            result.put("msg", "仅候选人可查询收藏");
            return result;
        }

        long pageNo = current != null && current > 0 ? current : 1;
        long pageSize = size != null && size > 0 ? Math.min(size, 100) : 20;

        Page<JobFavorite> page = page(
                new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<JobFavorite>()
                        .eq(JobFavorite::getCandidateId, candidateId)
                        .orderByDesc(JobFavorite::getCreatedAt));

        List<JobFavorite> favorites = page.getRecords();
        Set<Long> jobIds = favorites.stream()
                .map(JobFavorite::getJobId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, JobPost> jobMap = new HashMap<>();
        if (!jobIds.isEmpty()) {
            List<JobPost> jobs = jobPostService.listByIds(jobIds);
            for (JobPost job : jobs) {
                if (job != null && job.getId() != null) {
                    jobMap.put(job.getId(), job);
                }
            }
        }

        List<JobFavoriteVO> records = new ArrayList<>();
        for (JobFavorite favorite : favorites) {
            JobFavoriteVO vo = new JobFavoriteVO();
            vo.setFavoriteId(favorite.getId());
            vo.setJobId(favorite.getJobId());
            vo.setFavoritedAt(favorite.getCreatedAt());
            vo.setJob(jobMap.get(favorite.getJobId()));
            records.add(vo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("pages", page.getPages());
        result.put("code", 200);
        result.put("msg", "查询成功");
        result.put("data", data);
        return result;
    }
}
