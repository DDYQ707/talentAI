package com.talent.agent.chat.agent;

import com.talent.agent.constant.ChatPromptConstants;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface HrAssistant {

    @SystemMessage(ChatPromptConstants.HR_ASSISTANT_SYSTEM)
    String chat(@MemoryId Long sessionId, @UserMessage String userMessage);
}
