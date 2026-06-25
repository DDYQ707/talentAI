package com.talent.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 职位强制下架请求
 *
 * @author TalentAI
 */
@Data
public class JobTakedownRequest {

    /** 下架原因（必填） */
    @NotBlank(message = "下架原因不能为空")
    private String reason;
}