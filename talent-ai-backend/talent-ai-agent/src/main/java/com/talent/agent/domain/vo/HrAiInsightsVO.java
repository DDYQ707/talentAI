package com.talent.agent.domain.vo;

import lombok.Builder;
import lombok.Getter;

/** HR 工作台 — AI 今日洞察 */
@Getter
@Builder
public class HrAiInsightsVO {

    /** 今日 AI 解析成功数 */
    private long parsedToday;

    /** 本月 AI 解析成功数 */
    private long parsedThisMonth;

    /** 本月高匹配候选人数（matchScore >= 80 且匹配成功） */
    private long highMatchCount;

    /** 本月智能匹配成功数 */
    private long matchSuccessThisMonth;

    /** 待处理 AI 任务数（解析/匹配 待处理+处理中） */
    private long pendingAiTasks;

    /** 今日 AI 任务失败数（解析+匹配） */
    private long failedToday;

    /** 招聘健康度 0-100（基于 AI 解析/匹配成功率） */
    private int healthScore;

    /** 健康度标签：优秀/良好/一般/待改善 */
    private String healthLabel;

    /** 摘要文案 */
    private String summary;
}
