package com.talent.agent.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InterviewQuestionVO {

    private Long id;

    private Long interviewId;

    private String questionText;

    private String category;

    private String focusPoint;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
