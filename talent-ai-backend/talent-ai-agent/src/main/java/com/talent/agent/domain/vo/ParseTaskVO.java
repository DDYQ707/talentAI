package com.talent.agent.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ParseTaskVO {

    private Long taskId;

    private Long resumeId;

    private Long attachmentId;

    /** 0-待处理 1-处理中 2-成功 3-失败 */
    private Integer taskStatus;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
