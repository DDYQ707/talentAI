package com.talent.job.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.job.dto.AdminJobTakedownRequest;
import com.talent.job.entity.JobPost;
import com.talent.job.service.IJobPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端内部接口：风控下架同步真实岗位
 */
@RestController
@RequestMapping("/api/job/internal/admin")
@RequiredArgsConstructor
public class JobInternalAdminController {

    private final IJobPostService jobPostService;

    @PutMapping("/takedown")
    public Map<String, Object> adminTakedown(@RequestBody AdminJobTakedownRequest request) {
        Map<String, Object> result = new HashMap<>();
        if (request == null) {
            result.put("code", 400);
            result.put("msg", "请求体不能为空");
            return result;
        }

        JobPost job = resolveJob(request);
        if (job == null) {
            result.put("code", 404);
            result.put("msg", "未找到匹配的招聘岗位");
            return result;
        }

        if (job.getStatus() != null && job.getStatus() == 2) {
            result.put("code", 200);
            result.put("msg", "岗位已处于暂停状态");
            result.put("data", Map.of("jobPostId", job.getId()));
            return result;
        }

        job.setStatus((byte) 2);
        job.setClosedAt(LocalDateTime.now());
        boolean ok = jobPostService.updateById(job);
        if (!ok) {
            result.put("code", 500);
            result.put("msg", "岗位下架失败");
            return result;
        }

        result.put("code", 200);
        result.put("msg", "岗位已同步下架");
        result.put("data", Map.of("jobPostId", job.getId()));
        return result;
    }

    private JobPost resolveJob(AdminJobTakedownRequest request) {
        if (request.getJobPostId() != null) {
            return jobPostService.getById(request.getJobPostId());
        }
        if (!StringUtils.hasText(request.getJobTitle())) {
            return null;
        }
        LambdaQueryWrapper<JobPost> w = new LambdaQueryWrapper<>();
        w.eq(JobPost::getTitle, request.getJobTitle().trim());
        if (request.getPublisherId() != null) {
            w.eq(JobPost::getPublisherId, request.getPublisherId());
        }
        w.orderByDesc(JobPost::getCreatedAt);
        w.last("LIMIT 1");
        return jobPostService.getOne(w, false);
    }
}
