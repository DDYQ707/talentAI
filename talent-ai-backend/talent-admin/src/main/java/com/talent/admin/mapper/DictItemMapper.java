package com.talent.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.admin.entity.DictItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据字典键值 Mapper
 *
 * @author TalentAI
 */
@Mapper
public interface DictItemMapper extends BaseMapper<DictItem> {
}