package com.talent.admin.dto.permission;

import lombok.Data;

import java.util.List;

/**
 * 权限模块分组 VO
 *
 * @author TalentAI
 */
@Data
public class PermModuleVO {

    private String module;
    private List<PermItemVO> perms;
}