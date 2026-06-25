package com.talent.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 模型启用 / 禁用请求
 *
 * @author TalentAI
 */
@Data
public class AiModelStatusRequest {

    /** 1-运行中（启用） 2-暂停（禁用） */
    @NotNull(message = "状态不能为空")
    private Integer status;
}