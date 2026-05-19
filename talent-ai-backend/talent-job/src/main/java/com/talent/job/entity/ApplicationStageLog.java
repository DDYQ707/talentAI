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
 * 投递阶段流转日志
 * </p>
 *
 * @author TalentAI
 * @since 2026-05-19
 */
@Getter
@Setter
@TableName("application_stage_log")
public class ApplicationStageLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 投递ID
     */
    private Long applicationId;

    /**
     * 原阶段
     */
    private Byte fromStage;

    /**
     * 新阶段
     */
    private Byte toStage;

    /**
     * 操作人ID（逻辑外键）
     */
    private Long operatorId;

    /**
     * 操作人姓名快照
     */
    private String operatorName;

    /**
     * 操作说明
     */
    private String actionNote;

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
