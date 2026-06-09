package com.talent.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 面试评价表（talent-interview 库）
 */
@Getter
@Setter
@TableName("interview_evaluation")
public class InterviewEvaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long interviewId;

    private Long evaluatorId;

    private String evaluatorName;

    /** 多维评分 JSON */
    private String dimensionScores;

    private BigDecimal overallScore;

    /** 1-通过 2-待定 3-不通过 */
    private Integer conclusion;

    private String comment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
