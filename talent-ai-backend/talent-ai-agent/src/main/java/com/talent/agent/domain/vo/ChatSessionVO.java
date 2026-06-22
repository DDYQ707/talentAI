package com.talent.agent.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatSessionVO {

    private Long id;

    private String sessionTitle;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
