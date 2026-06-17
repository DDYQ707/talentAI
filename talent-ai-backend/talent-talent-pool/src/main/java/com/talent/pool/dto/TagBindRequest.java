package com.talent.pool.dto;

import lombok.Data;

import java.util.List;

/**
 * 为候选人打标签请求体
 */
@Data
public class TagBindRequest {

    /**
     * 标签ID列表（批量绑定）
     */
    private List<Long> tagIds;
}
