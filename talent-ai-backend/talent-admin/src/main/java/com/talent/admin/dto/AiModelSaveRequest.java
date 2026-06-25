package com.talent.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 模型新增 / 编辑请求
 *
 * @author TalentAI
 */
@Data
public class AiModelSaveRequest {

    /** 模型编码（唯一） */
    @NotBlank(message = "模型编码不能为空")
    private String modelCode;

    /** 展示名称 */
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    /** 1-语言 2-嵌入 3-文档解析 */
    @NotNull(message = "模型类型不能为空")
    private Integer modelType;

    /** 使用场景 */
    private String purpose;

    /** API 地址 */
    private String apiEndpoint;

    /** 提示词模板 */
    private String promptTemplate;

    /** 1-运行中 2-暂停，默认 1 */
    private Integer status;
}