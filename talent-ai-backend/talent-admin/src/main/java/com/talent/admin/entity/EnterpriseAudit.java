package com.talent.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 企业资质审核表
 *
 * @author TalentAI
 */
@Getter
@Setter
@TableName("talent_enterprise_audit")
public class EnterpriseAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 企业名称 */
    private String companyName;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 法定代表人 */
    private String legalPerson;

    /** 注册资本 */
    private String registeredCapital;

    /** 经营范围 */
    private String businessScope;

    /** 营业执照图片URL */
    private String licenseUrl;

    /** 状态：0=待审核, 1=已通过, 2=已驳回 */
    private Integer status;

    /** 驳回理由 */
    private String rejectReason;

    /** 提交时间 */
    private LocalDateTime submittedAt;

    /** 审核时间 */
    private LocalDateTime reviewedAt;
}