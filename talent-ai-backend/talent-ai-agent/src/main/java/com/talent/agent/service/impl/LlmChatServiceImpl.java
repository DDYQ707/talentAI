package com.talent.agent.service.impl;

import com.talent.agent.config.DashScopeProperties;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.LlmChatService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmChatServiceImpl implements LlmChatService {

    private final DashScopeProperties dashScopeProperties;

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(dashScopeProperties.getApiKey())) {
            throw new AgentBusinessException("DASHSCOPE_API_KEY 未配置，请设置环境变量后重试");
        }
        if (!StringUtils.hasText(systemPrompt) || !StringUtils.hasText(userPrompt)) {
            throw new AgentBusinessException("systemPrompt 与 userPrompt 不能为空");
        }

        String model = dashScopeProperties.getModel();
        log.info("LLM chat request, model={}, baseUrl={}", model, dashScopeProperties.getBaseUrl());

        try {
            ChatLanguageModel chatModel = OpenAiChatModel.builder()
                    .baseUrl(dashScopeProperties.getBaseUrl())
                    .apiKey(dashScopeProperties.getApiKey())
                    .modelName(model)
                    .timeout(Duration.ofSeconds(dashScopeProperties.getTimeoutSeconds()))
                    .logRequests(false)
                    .logResponses(false)
                    .build();

            Response<AiMessage> response = chatModel.generate(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(userPrompt));
            String content = response.content().text();
            if (!StringUtils.hasText(content)) {
                throw new AgentBusinessException("大模型返回内容为空");
            }
            return content.trim();
        } catch (AgentBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM chat failed, model={}", model, e);
            throw new AgentBusinessException("大模型调用失败：" + e.getMessage(), e);
        }
    }
}
