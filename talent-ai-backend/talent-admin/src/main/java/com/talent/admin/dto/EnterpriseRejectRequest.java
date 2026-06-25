package com.talent.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 企业资质驳回请求
 *
 * @author TalentAI
 */
@Data
public class EnterpriseRejectRequest {

    /** 驳回理由（必填） */
    @NotBlank(message = "驳回理由不能为空")
    private String rejectReason;
}