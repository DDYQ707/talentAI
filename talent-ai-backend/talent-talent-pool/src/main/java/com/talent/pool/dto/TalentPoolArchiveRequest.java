package com.talent.pool.dto;

import lombok.Data;

/**
 * 人才归档入库请求体
 */
@Data
public class TalentPoolArchiveRequest {

    /**
     * 候选人ID（必填）
     */
    private Long candidateId;

    /**
     * 候选人姓名（必填）
     */
    private String candidateName;

    /**
     * 当前职位快照
     */
    private String currentTitle;

    /**
     * 简历ID
     */
    private Long resumeId;

    /**
     * 来源投递ID
     */
    private Long sourceApplicationId;

    /**
     * 人才分类
     */
    private Byte talentCategory;

    /**
     * 求职状态
     */
    private Byte jobSeekingStatus;

    /**
     * 归档时匹配分
     */
    private Byte matchScore;

    /**
     * 当前公司
     */
    private String currentCompany;

    /**
     * 归档原因
     */
    private String archiveReason;
}
