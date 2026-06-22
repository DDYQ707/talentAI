package com.talent.agent.service;

import com.talent.agent.domain.dto.ChatTurn;
import java.util.List;

/**
 * 大模型对话服务
 */
public interface LlmChatService {

    /**
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 模型回复文本
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 多轮对话（history 不含本次 userMessage）
     */
    String chatWithHistory(String systemPrompt, List<ChatTurn> history, String userMessage);
}
