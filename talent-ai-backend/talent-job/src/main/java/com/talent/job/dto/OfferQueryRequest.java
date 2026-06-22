package com.talent.job.dto;

import lombok.Data;

/**
 * 分页查询 Offer 列表请求体
 */
@Data
public class OfferQueryRequest {

    /**
     * 当前页码（默认 1）
     */
    private Integer current = 1;

    /**
     * 每页大小（默认 10）
     */
    private Integer size = 10;

    /**
     * 状态过滤（可选）
     */
    private Byte status;

    /**
     * 候选人姓名模糊搜索（可选）
     */
    private String candidateName;

    /**
     * 岗位名称模糊搜索（可选）
     */
    private String jobTitle;

    /**
     * 岗位 ID 精确过滤（可选）
     */
    private Long jobId;
}
