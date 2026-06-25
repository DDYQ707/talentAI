package com.talent.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据字典类型表
 *
 * @author TalentAI
 */
@Getter
@Setter
@TableName("talent_dict_type")
public class DictType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 字典编码，如 job_industry */
    private String code;

    /** 字典名称，如 行业领域 */
    private String name;

    /** 状态：1=启用, 0=禁用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;
}