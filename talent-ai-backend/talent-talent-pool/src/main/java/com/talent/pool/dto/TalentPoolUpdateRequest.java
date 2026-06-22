package com.talent.pool.dto;

import lombok.Data;

/**
 * 人才库记录更新请求体
 */
@Data
public class TalentPoolUpdateRequest {

    /**
     * 当前职位
     */
    private String currentTitle;

    /**
     * 人才分类
     */
    private Byte talentCategory;

    /**
     * 求职状态
     */
    private Byte jobSeekingStatus;

    /**
     * 匹配分
     */
    private Byte matchScore;

    /**
     * 当前公司
     */
    private String currentCompany;

    /**
     * 是否收藏
     */
    private Boolean isSaved;

    /**
     * 归档原因
     */
    private String archiveReason;
}
