package com.talent.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.PageQuery;
import com.talent.admin.entity.Announcement;
import com.talent.admin.exception.BusinessException;
import com.talent.admin.mapper.AnnouncementMapper;
import com.talent.admin.service.IAnnouncementService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 系统公告 Announcement 服务实现
 *
 * @author TalentAI
 */
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement>
        implements IAnnouncementService {

    @Override
    public PageResult<Announcement> pageQuery(PageQuery query) {
        int pageNum = (query.getPage() != null && query.getPage() > 0) ? query.getPage() : 1;
        int pageSize = (query.getSize() != null && query.getSize() > 0) ? query.getSize() : 10;

        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getCreatedAt);

        Page<Announcement> p = page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(p);
    }

    @Override
    public void broadcast(Long id) {
        Announcement announcement = getExisting(id);
        if (Boolean.TRUE.equals(announcement.getBroadcasted())) {
            throw new BusinessException("该公告已广播，请勿重复操作");
        }
        Announcement update = new Announcement();
        update.setId(id);
        update.setBroadcasted(true);
        update.setBroadcastedAt(LocalDateTime.now());
        if (!updateById(update)) {
            throw new BusinessException("广播失败");
        }
    }

    @Override
    public void remove(Long id) {
        getExisting(id);
        if (!removeById(id)) {
            throw new BusinessException("删除失败");
        }
    }

    private Announcement getExisting(Long id) {
        if (id == null) {
            throw new BusinessException(400, "公告ID不能为空");
        }
        Announcement announcement = getById(id);
        if (announcement == null) {
            throw new BusinessException(404, "公告不存在");
        }
        return announcement;
    }
}