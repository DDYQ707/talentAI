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
 * 招聘岗位表（talent-job库）
 * </p>
 *
 * @author TalentAI
 * @since 2026-05-19
 */
@Getter
@Setter
@TableName("job_post")
public class JobPost implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 岗位编号
     */
    private String jobCode;

    /**
     * 岗位名称
     */
    private String title;

    /**
     * 所属部门ID（逻辑外键->auth库）
     */
    private Long deptId;

    /**
     * 部门名称快照
     */
    private String deptName;

    /**
     * 发布人HR用户ID（逻辑外键->auth库）
     */
    private Long publisherId;

    /**
     * 发布人姓名快照
     */
    private String publisherName;

    /**
     * 1-招聘中 2-暂停 3-已完成
     */
    private Byte status;

    /**
     * 1-全职 2-兼职 3-实习
     */
    private Byte employmentType;

    /**
     * 工作城市
     */
    private String workCity;

    /**
     * 是否远程
     */
    private Boolean isRemote;

    /**
     * 薪资下限元/月
     */
    private Integer salaryMin;

    /**
     * 薪资上限元/月
     */
    private Integer salaryMax;

    /**
     * 是否面议
     */
    private Boolean salaryNegotiable;

    /**
     * 招聘人数
     */
    private Integer headcount;

    /**
     * 1-高 2-中 3-低
     */
    private Byte priority;

    /**
     * 最低工作年限
     */
    private Byte experienceYearsMin;

    /**
     * 学历要求
     */
    private Byte educationRequirement;

    /**
     * 职位描述JD
     */
    private String jobDescription;

    /**
     * 任职要求
     */
    private String jobRequirements;

    /**
     * 技能标签逗号分隔
     */
    private String skillTags;

    /**
     * 投递数
     */
    private Integer appliedCount;

    /**
     * 匹配通过数
     */
    private Integer matchedCount;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 关闭时间
     */
    private LocalDateTime closedAt;

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
