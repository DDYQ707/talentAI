package com.talent.resume.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenStatusUpdateRequest {

    /** 1-待初筛 2-面试中 3-已录用 4-已淘汰 5-待录用 */
    private Integer screenStatus;

    private String remark;

    /** 淘汰时是否同步归档人才库 */
    private Boolean archiveToTalentPool;

    private String archiveReason;

    private String interviewSummary;
}
