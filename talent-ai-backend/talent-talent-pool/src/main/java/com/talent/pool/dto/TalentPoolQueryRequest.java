package com.talent.pool.dto;

import lombok.Data;

/**
 * 人才大厅分页查询请求参数
 */
@Data
public class TalentPoolQueryRequest {

    /**
     * 求职状态筛选
     */
    private Byte jobSeekingStatus;

    /**
     * 匹配分数下限
     */
    private Integer minScore;

    /**
     * 匹配分数上限
     */
    private Integer maxScore;

    /**
     * 当前页码（默认1）
     */
    private Integer current;

    /**
     * 每页条数（默认20）
     */
    private Integer size;
}
