package com.talent.auth.dto;

import lombok.Data;

/**
 * 公告广播：按目标人群批量推送系统通知
 */
@Data
public class NotificationBroadcastRequest {

    private String title;

    private String content;

    /** candidate / hr / all */
    private String target;

    /** 关联公告 ID（可选） */
    private Long announcementId;
}
