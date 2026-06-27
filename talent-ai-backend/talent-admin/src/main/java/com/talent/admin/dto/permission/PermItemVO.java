package com.talent.admin.dto.permission;

import lombok.Data;

/**
 * 权限项 VO
 *
 * @author TalentAI
 */
@Data
public class PermItemVO {

    private Long id;
    private String permCode;
    private String permName;
}