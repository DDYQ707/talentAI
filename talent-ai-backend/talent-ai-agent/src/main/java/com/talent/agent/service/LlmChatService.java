package com.talent.agent.service;

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
}
