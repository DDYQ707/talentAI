package com.talent.pool.dto;

import lombok.Data;

/**
 * 创建标签请求体
 */
@Data
public class TalentTagCreateRequest {

    /**
     * 标签名（必填）
     */
    private String tagName;

    /**
     * 标签类型：1-技能 2-领域 3-自定义（默认1）
     */
    private Byte tagType;
}
