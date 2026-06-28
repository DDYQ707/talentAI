package com.talent.job.dto;

import lombok.Data;

/**
 * 管理端强制下架岗位（内部 API）
 */
@Data
public class AdminJobTakedownRequest {

    /** 岗位 ID（优先） */
    private Long jobPostId;

    /** 岗位名称（jobPostId 为空时按标题匹配） */
    private String jobTitle;

    /** 发布者 ID（可选，缩小匹配范围） */
    private Long publisherId;

    /** 下架原因 */
    private String reason;
}
