package com.talent.admin.service;

import com.talent.admin.dto.permission.PermModuleVO;
import com.talent.admin.dto.permission.RolePermissionRequest;
import com.talent.admin.dto.permission.RoleSaveRequest;
import com.talent.admin.dto.permission.RoleVO;
import com.talent.admin.entity.SysRole;

import java.util.List;

/**
 * 权限管理 服务
 *
 * @author TalentAI
 */
public interface IPermissionService {

    /** 角色列表（含用户数、权限数） */
    List<RoleVO> listRoles();

    /** 权限项按模块分组 */
    List<PermModuleVO> listPermTree();

    /** 某角色已有的权限ID列表 */
    List<Long> listRolePermissionIds(Long roleId);

    /** 保存角色权限 */
    void saveRolePermissions(Long roleId, RolePermissionRequest request);

    /** 新建角色 */
    SysRole createRole(RoleSaveRequest request);

    /** 编辑角色 */
    void updateRole(Long id, RoleSaveRequest request);

    /** 删除角色（逻辑删，并清理权限关联） */
    void deleteRole(Long id);
}