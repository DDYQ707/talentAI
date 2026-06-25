package com.talent.admin.dto;

import lombok.Data;

/**
 * AI 模型列表查询条件
 *
 * @author TalentAI
 */
@Data
public class AiModelQuery {

    /** 页码，默认 1 */
    private Integer page;

    /** 每页条数，默认 10 */
    private Integer size;

    /** 关键字：模型名称 / 模型编码 模糊匹配 */
    private String keyword;

    /** 状态：1-运行中 2-暂停 */
    private Integer status;

    /** 类型：1-语言 2-嵌入 3-文档解析 */
    private Integer modelType;
}