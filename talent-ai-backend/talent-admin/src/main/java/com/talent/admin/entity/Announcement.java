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
 * 系统公告 Announcement 表
 *
 * @author TalentAI
 */
@Getter
@Setter
@TableName("announcement")
public class Announcement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 级别：info / warning / critical */
    private String level;

    /** 目标人群：candidate / hr / all */
    private String target;

    /** 是否已广播 */
    private Boolean broadcasted;

    /** 广播时间 */
    @TableField("broadcasted_at")
    private LocalDateTime broadcastedAt;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}