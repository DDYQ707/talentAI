package com.talent.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.admin.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 角色 Mapper（auth 库）
 *
 * @author TalentAI
 */
@Mapper
@DS("auth")
public interface PermRoleMapper extends BaseMapper<SysRole> {

    /** 角色下的用户数 */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId} AND is_deleted = 0")
    long countUsers(@Param("roleId") Long roleId);

    /** 角色拥有的权限数 */
    @Select("SELECT COUNT(*) FROM sys_role_permission WHERE role_id = #{roleId} AND is_deleted = 0")
    long countPerms(@Param("roleId") Long roleId);
}