package com.talent.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 轮播图 Banner 表
 *
 * @author TalentAI
 */
@Getter
@Setter
@TableName("banner")
public class Banner implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 图片URL */
    @TableField("image_url")
    private String imageUrl;

    /** 跳转链接 */
    @TableField("link_url")
    private String linkUrl;

    /** 开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 状态：0=下线, 1=上线 */
    private Integer status;

    /** 排序值，越小越靠前 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}