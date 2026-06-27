package com.talent.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.admin.dto.permission.PermItemVO;
import com.talent.admin.dto.permission.PermModuleVO;
import com.talent.admin.dto.permission.RolePermissionRequest;
import com.talent.admin.dto.permission.RoleSaveRequest;
import com.talent.admin.dto.permission.RoleVO;
import com.talent.admin.entity.SysPermission;
import com.talent.admin.entity.SysRole;
import com.talent.admin.exception.BusinessException;
import com.talent.admin.mapper.PermPermissionMapper;
import com.talent.admin.mapper.PermRoleMapper;
import com.talent.admin.mapper.PermRolePermissionMapper;
import com.talent.admin.service.IPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理 服务实现
 *
 * @author TalentAI
 */
@Service
@DS("auth")
@RequiredArgsConstructor
public class PermissionServiceImpl implements IPermissionService {

    private final PermRoleMapper roleMapper;
    private final PermPermissionMapper permissionMapper;
    private final PermRolePermissionMapper rolePermissionMapper;

    @Override
    public List<RoleVO> listRoles() {
        List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getIsDeleted, 0)
                        .orderByAsc(SysRole::getSortOrder)
                        .orderByAsc(SysRole::getId));
        List<RoleVO> list = new ArrayList<>();
        for (SysRole role : roles) {
            RoleVO vo = new RoleVO();
            vo.setId(role.getId());
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
            vo.setDescription(role.getDescription());
            vo.setStatus(role.getStatus());
            vo.setUserCount(roleMapper.countUsers(role.getId()));
            vo.setPermCount(roleMapper.countPerms(role.getId()));
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<PermModuleVO> listPermTree() {
        List<SysPermission> perms = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getIsDeleted, 0)
                        .orderByAsc(SysPermission::getSortOrder)
                        .orderByAsc(SysPermission::getId));
        Map<String, PermModuleVO> moduleMap = new LinkedHashMap<>();
        for (SysPermission perm : perms) {
            String module = perm.getModuleName();
            PermModuleVO moduleVO = moduleMap.computeIfAbsent(module, m -> {
                PermModuleVO vo = new PermModuleVO();
                vo.setModule(m);
                vo.setPerms(new ArrayList<>());
                return vo;
            });
            PermItemVO item = new PermItemVO();
            item.setId(perm.getId());
            item.setPermCode(perm.getPermCode());
            item.setPermName(perm.getPermName());
            moduleVO.getPerms().add(item);
        }
        return new ArrayList<>(moduleMap.values());
    }

    @Override
    public List<Long> listRolePermissionIds(Long roleId) {
        if (roleId == null) {
            throw new BusinessException(400, "角色ID不能为空");
        }
        return rolePermissionMapper.selectPermissionIds(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(Long roleId, RolePermissionRequest request) {
        SysRole role = requireRole(roleId);
        // 先删旧关联（物理删，避免唯一键冲突）
        rolePermissionMapper.deleteByRoleId(role.getId());
        // 再批量插入新关联
        if (request != null && !CollectionUtils.isEmpty(request.getPermissionIds())) {
            for (Long permissionId : request.getPermissionIds()) {
                if (permissionId != null) {
                    rolePermissionMapper.insertOne(role.getId(), permissionId);
                }
            }
        }
    }

    @Override
    public SysRole createRole(RoleSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getRoleCode())) {
            throw new BusinessException(400, "角色编码不能为空");
        }
        if (!StringUtils.hasText(request.getRoleName())) {
            throw new BusinessException(400, "角色名称不能为空");
        }
        long exist = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, request.getRoleCode()));
        if (exist > 0) {
            throw new BusinessException("角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(1);
        role.setSortOrder(0);
        role.setIsDeleted(0);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(role);
        return role;
    }

    @Override
    public void updateRole(Long id, RoleSaveRequest request) {
        SysRole exist = requireRole(id);
        if (request != null && StringUtils.hasText(request.getRoleCode())
                && !request.getRoleCode().equals(exist.getRoleCode())) {
            long c = roleMapper.selectCount(
                    new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, request.getRoleCode()));
            if (c > 0) {
                throw new BusinessException("角色编码已存在");
            }
        }
        SysRole update = new SysRole();
        update.setId(id);
        if (request != null) {
            update.setRoleCode(request.getRoleCode());
            update.setRoleName(request.getRoleName());
            update.setDescription(request.getDescription());
        }
        update.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = requireRole(id);
        // 清理权限关联
        rolePermissionMapper.deleteByRoleId(role.getId());
        // 逻辑删角色
        roleMapper.deleteById(role.getId());
    }

    private SysRole requireRole(Long id) {
        if (id == null) {
            throw new BusinessException(400, "角色ID不能为空");
        }
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return role;
    }
}