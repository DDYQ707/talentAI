package com.talent.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 模型管理 顶部统计卡
 *
 * @author TalentAI
 */
@Data
public class ModelStatsVO {

    /** 活跃模型数（status=1 且未删除） */
    private Long activeModels;

    /** 今日调用量（ai_parse_task + ai_match_record 今日记录数） */
    private Long todayCalls;

    /** Token 消耗总量（ai_match_record.token_used 求和） */
    private Long totalTokens;

    /** 估算费用（totalTokens × 0.000002，保留2位） */
    private BigDecimal estimatedCost;

    /** 平均延迟（秒，两表 finished_at/started_at 非空记录的平均耗时） */
    private BigDecimal avgLatency;
}