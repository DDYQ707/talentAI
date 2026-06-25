package com.talent.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.PageQuery;
import com.talent.admin.entity.Banner;
import com.talent.admin.exception.BusinessException;
import com.talent.admin.mapper.BannerMapper;
import com.talent.admin.service.IBannerService;
import org.springframework.stereotype.Service;

/**
 * 轮播图 Banner 服务实现
 *
 * @author TalentAI
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements IBannerService {

    @Override
    public PageResult<Banner> pageQuery(PageQuery query) {
        int pageNum = (query.getPage() != null && query.getPage() > 0) ? query.getPage() : 1;
        int pageSize = (query.getSize() != null && query.getSize() > 0) ? query.getSize() : 10;

        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSortOrder).orderByDesc(Banner::getCreatedAt);

        Page<Banner> p = page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(p);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值不合法，只能为 0 或 1");
        }
        getExisting(id);
        Banner update = new Banner();
        update.setId(id);
        update.setStatus(status);
        if (!updateById(update)) {
            throw new BusinessException("状态更新失败");
        }
    }

    @Override
    public void remove(Long id) {
        getExisting(id);
        if (!removeById(id)) {
            throw new BusinessException("删除失败");
        }
    }

    private Banner getExisting(Long id) {
        if (id == null) {
            throw new BusinessException(400, "轮播图ID不能为空");
        }
        Banner banner = getById(id);
        if (banner == null) {
            throw new BusinessException(404, "轮播图不存在");
        }
        return banner;
    }
}