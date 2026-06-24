package com.talent.job.controller;

import com.talent.job.service.IJobFavoriteService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 候选人岗位收藏（网关路由：/api/delivery/favorites/**）
 */
@RestController
@RequestMapping("/api/delivery/favorites")
public class JobFavoriteController {

    @Autowired
    private IJobFavoriteService jobFavoriteService;

    /** 我的收藏列表（分页，含岗位详情） */
    @GetMapping("/my")
    public Map<String, Object> myFavorites(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return jobFavoriteService.listMyFavorites(userId, userRole, current, size);
    }

    /** 已收藏岗位 ID 列表（用于列表页标记收藏状态） */
    @GetMapping("/job-ids")
    public Map<String, Object> favoriteJobIds(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        return jobFavoriteService.listFavoriteJobIds(userId, userRole);
    }

    /** 切换收藏状态 */
    @PutMapping("/{jobId}/toggle")
    public Map<String, Object> toggleFavorite(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable("jobId") Long jobId) {
        return jobFavoriteService.toggleFavorite(userId, userRole, jobId);
    }
}
