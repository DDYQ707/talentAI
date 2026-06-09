package com.talent.interview.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewDetailVO extends InterviewListVO {

    private Long createdBy;

    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private InterviewEvaluationVO evaluation;
}
