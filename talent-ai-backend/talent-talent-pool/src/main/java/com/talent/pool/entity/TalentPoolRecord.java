package com.talent.pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 人才库记录表（talent-pool库）
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-17
 */
@Getter
@Setter
@TableName("talent_pool_record")
public class TalentPoolRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 候选人ID（逻辑外键）
     */
    private Long candidateId;

    /**
     * 候选人姓名快照
     */
    private String candidateName;

    /**
     * 当前职位快照
     */
    private String currentTitle;

    /**
     * 简历ID（逻辑外键）
     */
    private Long resumeId;

    /**
     * 来源投递ID
     */
    private Long sourceApplicationId;

    /**
     * 来源岗位名称快照
     */
    private String sourceJobTitle;

    /**
     * 面试评价摘要
     */
    private String interviewSummary;

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
     * 是否收藏
     */
    private Boolean isSaved;

    /**
     * 归档原因
     */
    private String archiveReason;

    /**
     * 操作HR
     */
    private Long archivedBy;

    /**
     * 归档时间
     */
    private LocalDateTime archivedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-否 1-是
     */
    @TableLogic
    private Boolean isDeleted;
}
