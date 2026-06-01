package com.talent.agent.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ai_match_record")
public class AiMatchRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private Long jobId;

    private Long resumeId;

    private Long modelId;

    private Integer matchScore;

    /** 0-待处理 1-处理中 2-成功 3-失败 */
    private Integer matchStatus;

    private String matchLevel;

    private String matchReason;

    private String advantages;

    private String disadvantages;

    private String suggestedQuestions;

    private String dimensionScores;

    private Integer tokenUsed;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
