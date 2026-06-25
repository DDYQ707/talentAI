package com.talent.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.PageQuery;
import com.talent.admin.entity.Announcement;

/**
 * 系统公告 Announcement 服务接口
 *
 * @author TalentAI
 */
public interface IAnnouncementService extends IService<Announcement> {

    /**
     * 分页查询
     */
    PageResult<Announcement> pageQuery(PageQuery query);

    /**
     * 标记为已广播
     */
    void broadcast(Long id);

    /**
     * 删除公告
     */
    void remove(Long id);
}