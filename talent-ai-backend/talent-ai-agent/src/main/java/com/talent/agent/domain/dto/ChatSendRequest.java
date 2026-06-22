package com.talent.agent.domain.dto;

import lombok.Data;

@Data
public class ChatSendRequest {

    private Long sessionId;

    private String message;
}
