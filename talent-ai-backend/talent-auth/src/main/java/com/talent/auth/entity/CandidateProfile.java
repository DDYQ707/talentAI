package com.talent.auth.entity;

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
 * 候选人扩展档案表
 * </p>
 *
 * @author TalentAI
 * @since 2026-05-19
 */
@Getter
@Setter
@TableName("candidate_profile")
public class CandidateProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 候选人用户ID
     */
    private Long userId;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 性别：0-未知 1-男 2-女
     */
    private Byte gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 当前职位
     */
    private String currentTitle;

    /**
     * 工作年限
     */
    private BigDecimal workYears;

    /**
     * 最高学历：1-大专 2-本科 3-硕士 4-博士
     */
    private Byte highestEdu;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 求职状态：1-在职观望 2-主动求职 3-被动求职
     */
    private Byte jobSeekingStatus;

    /**
     * 简历完整度0-100
     */
    private Byte resumeCompleteness;

    /**
     * AI简历评分0-100
     */
    private Byte aiScore;

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
