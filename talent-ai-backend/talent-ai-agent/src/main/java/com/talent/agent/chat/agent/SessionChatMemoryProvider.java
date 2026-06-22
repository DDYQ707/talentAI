package com.talent.agent.chat.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.constant.ChatConstants;
import com.talent.agent.domain.entity.AiChatMessage;
import com.talent.agent.mapper.AiChatMessageMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionChatMemoryProvider implements ChatMemoryProvider {

    private final AiChatMessageMapper messageMapper;

    @Override
    public ChatMemory get(Object memoryId) {
        MessageWindowChatMemory memory =
                MessageWindowChatMemory.withMaxMessages(ChatConstants.MAX_HISTORY_MESSAGES);
        if (!(memoryId instanceof Long sessionId) || sessionId <= 0) {
            return memory;
        }

        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .orderByAsc(AiChatMessage::getCreatedAt)
                        .last("LIMIT " + ChatConstants.MAX_HISTORY_MESSAGES));
        if (messages == null || messages.isEmpty()) {
            return memory;
        }

        for (AiChatMessage message : messages) {
            ChatMessage chatMessage = toChatMessage(message);
            if (chatMessage != null) {
                memory.add(chatMessage);
            }
        }
        return memory;
    }

    private ChatMessage toChatMessage(AiChatMessage message) {
        if (message == null || message.getContent() == null) {
            return null;
        }
        if (message.getRole() == ChatConstants.ROLE_USER) {
            return UserMessage.from(message.getContent());
        }
        if (message.getRole() == ChatConstants.ROLE_ASSISTANT) {
            return AiMessage.from(message.getContent());
        }
        return null;
    }
}
