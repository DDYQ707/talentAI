package com.talent.pool.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 人才大厅列表项视图
 */
@Data
public class TalentPoolRecordVO {

    private Long id;

    private Long candidateId;

    private String candidateName;

    private String currentTitle;

    private Long resumeId;

    private Long sourceApplicationId;

    private Byte talentCategory;

    private Byte jobSeekingStatus;

    private Byte matchScore;

    private String currentCompany;

    private Boolean isSaved;

    private String archiveReason;

    private Long archivedBy;

    private LocalDateTime archivedAt;

    private LocalDateTime createdAt;

    /**
     * 关联标签列表
     */
    private List<TalentTagVO> tags;
}
