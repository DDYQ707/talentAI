package com.talent.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.PageQuery;
import com.talent.admin.entity.Banner;

/**
 * 轮播图 Banner 服务接口
 *
 * @author TalentAI
 */
public interface IBannerService extends IService<Banner> {

    /**
     * 分页查询
     */
    PageResult<Banner> pageQuery(PageQuery query);

    /**
     * 更新上下线状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 删除轮播图
     */
    void remove(Long id);
}