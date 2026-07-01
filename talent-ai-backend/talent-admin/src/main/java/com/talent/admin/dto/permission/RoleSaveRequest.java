package com.talent.admin.dto.permission;

import lombok.Data;

/**
 * 角色新建/编辑请求
 *
 * @author TalentAI
 */
@Data
public class RoleSaveRequest {

    private String roleCode;
    private String roleName;
    private String description;
}