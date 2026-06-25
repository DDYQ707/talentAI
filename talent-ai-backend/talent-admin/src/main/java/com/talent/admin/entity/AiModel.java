package com.talent.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 模型配置表（位于 talent_ai_db，通过 @DS("ai") 访问）
 *
 * @author TalentAI
 */
@Getter
@Setter
@TableName("ai_model")
public class AiModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 模型编码 */
    @TableField("model_code")
    private String modelCode;

    /** 展示名称 */
    @TableField("model_name")
    private String modelName;

    /** 1-语言 2-嵌入 3-文档解析 */
    @TableField("model_type")
    private Integer modelType;

    /** 使用场景 */
    private String purpose;

    /** API 地址 */
    @TableField("api_endpoint")
    private String apiEndpoint;

    /** 提示词模板（仅存储，不联动 ai-agent 真正生效） */
    @TableField("prompt_template")
    private String promptTemplate;

    /** 1-运行中 2-暂停 */
    private Integer status;

    @TableField(value = "created_at", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    /** 逻辑删除：0-未删 1-已删 */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}