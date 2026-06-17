package com.talent.pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 人才库标签关联表
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-17
 */
@Getter
@Setter
@TableName("talent_pool_tag")
public class TalentPoolTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 人才库记录ID
     */
    private Long poolRecordId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Boolean isDeleted;
}
