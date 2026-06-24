package com.talent.auth.dto;

import lombok.Data;

@Data
public class NotificationCreateRequest {

    private Long userId;

    private String title;

    private String content;

    /** 1-待办 2-提醒 3-公告 */
    private Byte notifyType;

    private String bizType;

    private Long bizId;
}
