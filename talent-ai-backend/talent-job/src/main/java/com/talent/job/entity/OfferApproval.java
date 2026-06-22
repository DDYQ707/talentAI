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
 * Offer 审批节点表（talent-job库）
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-14
 */
@Getter
@Setter
@TableName("offer_approval")
public class OfferApproval implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联 Offer ID（逻辑外键 → offer.id）
     */
    private Long offerId;

    /**
     * 审批顺序（1, 2, 3…）
     */
    private Integer seq;

    /**
     * 审批人用户 ID
     */
    private Long approverId;

    /**
     * 审批人姓名快照
     */
    private String approverName;

    /**
     * 1-待审批 2-已通过 3-已拒绝
     */
    private Byte status;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

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
