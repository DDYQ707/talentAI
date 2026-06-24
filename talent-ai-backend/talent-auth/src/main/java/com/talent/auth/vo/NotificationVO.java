package com.talent.auth.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NotificationVO {

    private Long id;

    private String title;

    private String content;

    private Byte notifyType;

    private String notifyTypeLabel;

    private String bizType;

    private Long bizId;

    private Boolean read;

    private LocalDateTime createdAt;
}
