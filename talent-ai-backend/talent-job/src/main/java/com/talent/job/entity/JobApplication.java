package com.talent.job.entity;

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
 * 投递申请表（talent-job库）
 * </p>
 *
 * @author TalentAI
 * @since 2026-05-19
 */
@Getter
@Setter
@TableName("job_application")
public class JobApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 投递单号
     */
    private String applicationNo;

    /**
     * 岗位ID
     */
    private Long jobId;

    /**
     * 岗位名称快照
     */
    private String jobTitle;

    /**
     * 候选人用户ID（逻辑外键->auth库）
     */
    private Long candidateId;

    /**
     * 候选人姓名快照
     */
    private String candidateName;

    /**
     * 简历ID（逻辑外键->resume库）
     */
    private Long resumeId;

    /**
     * 渠道：1-BOSS 2-猎头 3-内推 4-智联 5-其他
     */
    private Byte channel;

    /**
     * 当前招聘阶段
     */
    private Byte currentStage;

    /**
     * 1-进行中 2-已录用 3-已淘汰 4-已撤回
     */
    private Byte status;

    /**
     * AI匹配分0-100
     */
    private Byte matchScore;

    /**
     * HR标星
     */
    private Boolean isStarred;

    /**
     * 投递时间
     */
    private LocalDateTime appliedAt;

    /**
     * 淘汰时间
     */
    private LocalDateTime rejectedAt;

    /**
     * 录用时间
     */
    private LocalDateTime hiredAt;

    /**
     * HR备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Boolean isDeleted;
}
