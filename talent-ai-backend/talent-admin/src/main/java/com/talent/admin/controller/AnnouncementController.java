package com.talent.admin.controller;

import com.talent.admin.common.PageResult;
import com.talent.admin.common.Result;
import com.talent.admin.dto.PageQuery;
import com.talent.admin.entity.Announcement;
import com.talent.admin.service.IAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统公告 Announcement 控制器
 *
 * @author TalentAI
 */
@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final IAnnouncementService announcementService;

    /**
     * 分页查询
     */
    @GetMapping
    public Result<PageResult<Announcement>> page(PageQuery query) {
        return Result.success(announcementService.pageQuery(query));
    }

    /**
     * 标记为已广播
     */
    @PostMapping("/{id}/broadcast")
    public Result<Void> broadcast(@PathVariable Long id) {
        announcementService.broadcast(id);
        return Result.success();
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.remove(id);
        return Result.success();
    }
}