package com.talent.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限表（auth 库）
 *
 * @author TalentAI
 */
@Getter
@Setter
@TableName("sys_permission")
public class SysPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 父权限ID */
    private Long parentId;

    /** 权限编码 */
    private String permCode;

    /** 权限名称 */
    private String permName;

    /** 所属模块 */
    private String moduleName;

    /** 1-目录 2-操作 3-API */
    private Integer permType;

    /** 排序 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除：0-否 1-是 */
    private Integer isDeleted;
}