package com.talent.resume.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenStatusUpdateRequest {

    /** 1-待初筛 2-面试中 3-已录用 4-已淘汰 */
    private Integer screenStatus;

    private String remark;
}
