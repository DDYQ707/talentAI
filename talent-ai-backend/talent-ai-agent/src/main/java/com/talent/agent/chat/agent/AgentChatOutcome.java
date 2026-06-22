package com.talent.agent.chat.agent;

import com.talent.agent.domain.vo.ChatCandidateCardVO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgentChatOutcome {

    private String reply;

    private List<ChatCandidateCardVO> candidates;
}
