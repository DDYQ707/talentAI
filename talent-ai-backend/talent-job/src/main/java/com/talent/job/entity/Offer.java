package com.talent.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Offer 主记录表（talent-job库）
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-14
 */
@Getter
@Setter
@TableName("offer")
public class Offer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Offer 编号（如 OFR-20260614-0001）
     */
    private String offerNo;

    /**
     * 关联投递申请 ID（逻辑外键 → job_application.id）
     */
    private Long applicationId;

    /**
     * 关联岗位 ID（逻辑外键 → job_post.id）
     */
    private Long jobId;

    /**
     * 岗位名称快照
     */
    private String jobTitle;

    /**
     * 候选人用户 ID（逻辑外键 → auth 库）
     */
    private Long candidateId;

    /**
     * 候选人姓名快照
     */
    private String candidateName;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 部门名称快照
     */
    private String deptName;

    /**
     * 月度基本薪资
     */
    private BigDecimal baseSalary;

    /**
     * 年薪总包
     */
    private BigDecimal annualSalary;

    /**
     * 年终奖
     */
    private BigDecimal bonus;

    /**
     * 薪资补充说明（如股票期权、补贴等）
     */
    private String salaryRemark;

    /**
     * 职级（如 P6、M1）
     */
    private String positionLevel;

    /**
     * 预计入职日期
     */
    private LocalDate expectedOnboardDate;

    /**
     * 试用期月数
     */
    private Integer probationMonths;

    /**
     * 1-待审批 2-审批中 3-已通过 4-已拒绝 5-已发放 6-候选人已接受 7-候选人已拒绝 8-已撤回
     */
    private Byte status;

    /**
     * 发起 HR 用户 ID
     */
    private Long hrId;

    /**
     * 发起 HR 姓名快照
     */
    private String hrName;

    /**
     * Offer 备注
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
