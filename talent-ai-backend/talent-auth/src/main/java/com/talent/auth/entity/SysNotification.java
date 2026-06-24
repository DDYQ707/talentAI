package com.talent.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_notification")
public class SysNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    /** 1-待办 2-提醒 3-公告 */
    private Byte notifyType;

    private String bizType;

    private Long bizId;

    private Boolean isRead;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean isDeleted;
}
