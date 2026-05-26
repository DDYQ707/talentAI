package com.talent.job.dto;

import lombok.Getter;
import lombok.Setter;

/** 内部调用：按 resume.screen_status 同步最近一条投递记录 */
@Getter
@Setter
public class SyncScreenStatusRequest {

    private Long candidateId;

    /** 1-待初筛 2-面试中 3-已录用 4-已淘汰 */
    private Integer screenStatus;

    private Long operatorId;

    private String operatorName;

    private String remark;
}
