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
 * 面试安排表（talent-interview 库）
 */
@Getter
@Setter
@TableName("interview")
public class Interview implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private Long jobId;

    private Long candidateId;

    private String candidateName;

    private String jobTitle;

    private Long interviewerId;

    private String interviewerName;

    /** 轮次序号 */
    private Integer roundNo;

    /** 轮次类型：1-AI初筛 2-业务初试 3-业务复试 4-HR面 5-终面 6-交叉面 7-作品评审 */
    private Integer roundType;

    /** 1-视频 2-现场 3-线上评审 */
    private Integer interviewMode;

    private LocalDateTime scheduledStart;

    private LocalDateTime scheduledEnd;

    private String meetingUrl;

    private String location;

    /** 1-待进行 2-面试完成 3-待安排 4-已取消 */
    private Integer status;

    private BigDecimal totalScore;

    private Long createdBy;

    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
