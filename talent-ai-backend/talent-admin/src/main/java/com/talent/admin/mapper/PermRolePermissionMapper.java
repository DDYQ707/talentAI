package com.talent.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联 Mapper（auth 库）
 *
 * @author TalentAI
 */
@Mapper
@DS("auth")
public interface PermRolePermissionMapper {

    /** 查询某角色已分配的权限ID列表 */
    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId} AND is_deleted = 0")
    List<Long> selectPermissionIds(@Param("roleId") Long roleId);

    /** 物理删除某角色的全部权限关联 */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /** 插入单条角色-权限关联 */
    @Insert("INSERT INTO sys_role_permission (role_id, permission_id, is_deleted) VALUES (#{roleId}, #{permissionId}, 0)")
    int insertOne(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}