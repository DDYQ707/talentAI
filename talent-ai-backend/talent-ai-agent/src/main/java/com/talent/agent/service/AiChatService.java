package com.talent.agent.service;

import com.talent.agent.domain.dto.ChatSendRequest;
import com.talent.agent.domain.vo.ChatMessageVO;
import com.talent.agent.domain.vo.ChatSendResponseVO;
import com.talent.agent.domain.vo.ChatSessionVO;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {

    ChatSendResponseVO sendMessage(Long userId, ChatSendRequest request);

    SseEmitter streamMessage(Long userId, ChatSendRequest request);

    List<ChatSessionVO> listSessions(Long userId);

    List<ChatMessageVO> listMessages(Long userId, Long sessionId);
}
