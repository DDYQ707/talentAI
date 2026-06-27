package com.talent.admin.controller;

import com.talent.admin.common.Result;
import com.talent.admin.dto.permission.PermModuleVO;
import com.talent.admin.dto.permission.RolePermissionRequest;
import com.talent.admin.dto.permission.RoleSaveRequest;
import com.talent.admin.dto.permission.RoleVO;
import com.talent.admin.entity.SysRole;
import com.talent.admin.service.IPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理 控制器（RBAC，直连 auth 库）
 *
 * @author TalentAI
 */
@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final IPermissionService permissionService;

    /** 角色列表 */
    @GetMapping("/roles")
    public Result<List<RoleVO>> listRoles() {
        return Result.success(permissionService.listRoles());
    }

    /** 权限项（按模块分组） */
    @GetMapping("/tree")
    public Result<List<PermModuleVO>> tree() {
        return Result.success(permissionService.listPermTree());
    }

    /** 某角色已有的权限ID列表 */
    @GetMapping("/role/{roleId}")
    public Result<List<Long>> rolePermissions(@PathVariable Long roleId) {
        return Result.success(permissionService.listRolePermissionIds(roleId));
    }

    /** 保存角色权限 */
    @PutMapping("/role/{roleId}")
    public Result<Void> saveRolePermissions(@PathVariable Long roleId,
                                            @RequestBody RolePermissionRequest request) {
        permissionService.saveRolePermissions(roleId, request);
        return Result.success();
    }

    /** 新建角色 */
    @PostMapping("/roles")
    public Result<SysRole> createRole(@RequestBody RoleSaveRequest request) {
        return Result.success(permissionService.createRole(request));
    }

    /** 编辑角色 */
    @PutMapping("/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody RoleSaveRequest request) {
        permissionService.updateRole(id, request);
        return Result.success();
    }

    /** 删除角色（逻辑删，同时清理权限关联） */
    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        permissionService.deleteRole(id);
        return Result.success();
    }
}