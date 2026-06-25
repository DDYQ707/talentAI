package com.talent.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据字典键值表
 *
 * @author TalentAI
 */
@Getter
@Setter
@TableName("talent_dict_item")
public class DictItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联类型ID */
    private Long dictTypeId;

    /** 字典标签，如 互联网/IT */
    private String label;

    /** 字典键值，如 IT */
    private String value;

    /** 排序 */
    private Integer sort;

    /** 状态：1=启用, 0=禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}