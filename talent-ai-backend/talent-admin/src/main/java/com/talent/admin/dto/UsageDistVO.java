package com.talent.admin.dto;

import lombok.Data;

/**
 * 各模型调用分布项
 *
 * @author TalentAI
 */
@Data
public class UsageDistVO {

    /** 模型名称 */
    private String name;

    /** 调用次数 */
    private Long count;
}