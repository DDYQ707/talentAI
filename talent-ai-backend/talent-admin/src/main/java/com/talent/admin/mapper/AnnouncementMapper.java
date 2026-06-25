package com.talent.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.admin.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统公告 Announcement Mapper
 *
 * @author TalentAI
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}