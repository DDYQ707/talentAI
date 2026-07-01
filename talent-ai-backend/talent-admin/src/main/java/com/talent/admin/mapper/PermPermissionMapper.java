package com.talent.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.admin.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限 Mapper（auth 库）
 *
 * @author TalentAI
 */
@Mapper
@DS("auth")
public interface PermPermissionMapper extends BaseMapper<SysPermission> {
}