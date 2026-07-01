package com.talent.admin.dto.permission;

import lombok.Data;

/**
 * 角色列表项 VO
 *
 * @author TalentAI
 */
@Data
public class RoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private Long userCount;
    private Long permCount;
}