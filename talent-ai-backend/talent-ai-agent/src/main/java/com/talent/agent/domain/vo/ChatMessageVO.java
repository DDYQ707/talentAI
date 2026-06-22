package com.talent.agent.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatMessageVO {

    private Long id;

    /** 1-user 2-assistant */
    private Integer role;

    private String content;

    private LocalDateTime createdAt;
}
