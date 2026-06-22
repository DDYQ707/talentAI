package com.talent.agent.domain.vo;

import java.util.List;
import lombok.Data;

@Data
public class ChatSendResponseVO {

    private Long sessionId;

    private String sessionTitle;

    private String reply;

    private List<ChatCandidateCardVO> candidates;
}
