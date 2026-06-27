package com.talent.admin.dto.permission;

import lombok.Data;

import java.util.List;

/**
 * 角色权限保存请求
 *
 * @author TalentAI
 */
@Data
public class RolePermissionRequest {

    private List<Long> permissionIds;
}