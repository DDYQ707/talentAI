package com.talent.job.controller;

import com.talent.job.dto.JobApplicationSubmitRequest;
import com.talent.job.service.IJobApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 候选人投递简历（网关路由：/api/delivery/**）
 */
@RestController
@RequestMapping("/api/delivery")
public class JobDeliveryController {

    @Autowired
    private IJobApplicationService jobApplicationService;

    /**
     * 投递简历：创建投递单、写入阶段日志、递增岗位投递数
     */
    @PostMapping("/submit")
    public Map<String, Object> submit(
            @RequestBody JobApplicationSubmitRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return jobApplicationService.submitApplication(userId, userRole, request);
    }

    /**
     * 我的投递记录（分页）
     */
    @GetMapping("/my")
    public Map<String, Object> myApplications(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return jobApplicationService.listMyApplications(userId, userRole, current, size);
    }

    /** 进行中投递的岗位 ID 列表 */
    @GetMapping("/applied-jobs")
    public Map<String, Object> appliedJobs(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return jobApplicationService.listActiveAppliedJobIds(userId, userRole);
    }
}
