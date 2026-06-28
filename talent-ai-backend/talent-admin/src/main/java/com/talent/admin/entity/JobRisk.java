package com.talent.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位风控日志表
 *
 * @author TalentAI
 */
@Getter
@Setter
@TableName("talent_job_risk")
public class JobRisk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 职位名称 */
    private String jobTitle;

    /** 企业名称 */
    private String companyName;

    /** 发布者ID */
    private Long publisherId;

    /** 关联 talent-job 岗位 ID（可选，用于精确同步下架） */
    private Long jobPostId;

    /** 最低薪资 */
    private Integer salaryMin;

    /** 最高薪资 */
    private Integer salaryMax;

    /** 职位描述 */
    private String description;

    /** 状态：0=正常, 1=风险预警, 2=已下架 */
    private Integer status;

    /** 触发的风险高危词 */
    private String riskKeywords;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 下架时间 */
    private LocalDateTime takenDownAt;

    /** 下架操作人ID */
    private Long takenDownBy;
}