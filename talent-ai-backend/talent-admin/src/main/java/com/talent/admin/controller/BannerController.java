package com.talent.admin.controller;

import com.talent.admin.common.PageResult;
import com.talent.admin.common.Result;
import com.talent.admin.dto.BannerStatusRequest;
import com.talent.admin.dto.PageQuery;
import com.talent.admin.entity.Banner;
import com.talent.admin.service.IBannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 轮播图 Banner 控制器
 *
 * @author TalentAI
 */
@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class BannerController {

    private final IBannerService bannerService;

    /**
     * 分页查询
     */
    @GetMapping
    public Result<PageResult<Banner>> page(PageQuery query) {
        return Result.success(bannerService.pageQuery(query));
    }

    /**
     * 更新上下线状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody @Valid BannerStatusRequest request) {
        bannerService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.remove(id);
        return Result.success();
    }
}