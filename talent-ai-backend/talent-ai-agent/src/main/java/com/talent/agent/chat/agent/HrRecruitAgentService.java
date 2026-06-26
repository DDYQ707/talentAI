package com.talent.agent.chat.agent;

import com.talent.agent.chat.tool.ApplicationQueryTool;
import com.talent.agent.chat.tool.InterviewQueryTool;
import com.talent.agent.chat.tool.JobQueryTool;
import com.talent.agent.chat.tool.KnowledgeSearchTool;
import com.talent.agent.chat.tool.MatchQueryTool;
import com.talent.agent.chat.tool.ResumeSearchTool;
import com.talent.agent.chat.context.ChatCandidateContext;
import com.talent.agent.config.DashScopeProperties;
import com.talent.agent.exception.AgentBusinessException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrRecruitAgentService {

    private final DashScopeProperties dashScopeProperties;
    private final SessionChatMemoryProvider chatMemoryProvider;
    private final MatchQueryTool matchQueryTool;
    private final JobQueryTool jobQueryTool;
    private final ApplicationQueryTool applicationQueryTool;
    private final ResumeSearchTool resumeSearchTool;
    private final InterviewQueryTool interviewQueryTool;
    private final KnowledgeSearchTool knowledgeSearchTool;
    private final ChatCandidateContext chatCandidateContext;

    private volatile HrAssistant hrAssistant;
    private volatile HrAssistantStream hrAssistantStream;

    private void ensureAssistantInitialized() {
        if (hrAssistant != null && hrAssistantStream != null) {
            return;
        }
        if (!StringUtils.hasText(dashScopeProperties.getApiKey())) {
            throw new AgentBusinessException("DASHSCOPE_API_KEY 未配置，请设置环境变量后重试");
        }
        synchronized (this) {
            if (hrAssistant != null && hrAssistantStream != null) {
                return;
            }
            Object[] tools = {
                matchQueryTool,
                jobQueryTool,
                applicationQueryTool,
                resumeSearchTool,
                interviewQueryTool,
                knowledgeSearchTool
            };
            ChatLanguageModel chatModel = buildChatModel();
            hrAssistant = AiServices.builder(HrAssistant.class)
                    .chatLanguageModel(chatModel)
                    .chatMemoryProvider(chatMemoryProvider)
                    .tools(tools)
                    .build();
            hrAssistantStream = AiServices.builder(HrAssistantStream.class)
                    .chatLanguageModel(chatModel)
                    .streamingChatLanguageModel(buildStreamingChatModel())
                    .chatMemoryProvider(chatMemoryProvider)
                    .tools(tools)
                    .build();
        }
    }

    public AgentChatOutcome chat(Long sessionId, String userMessage) {
        if (sessionId == null || sessionId <= 0) {
            throw new IllegalArgumentException("sessionId 无效");
        }
        if (!StringUtils.hasText(userMessage)) {
            throw new IllegalArgumentException("message 不能为空");
        }
        ensureAssistantInitialized();
        chatCandidateContext.clear();
        try {
            log.info("HR agent chat, sessionId={}, messageLength={}", sessionId, userMessage.length());
            String reply = hrAssistant.chat(sessionId, userMessage.trim());
            return new AgentChatOutcome(reply, chatCandidateContext.snapshot());
        } catch (AgentBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("HR agent chat failed, sessionId={}", sessionId, e);
            throw new AgentBusinessException("大模型调用失败：" + e.getMessage(), e);
        } finally {
            chatCandidateContext.remove();
        }
    }

    public void streamChat(
            Long sessionId,
            String userMessage,
            Consumer<String> onToken,
            Consumer<AgentChatOutcome> onComplete,
            Consumer<Throwable> onError) {
        if (sessionId == null || sessionId <= 0) {
            throw new IllegalArgumentException("sessionId 无效");
        }
        if (!StringUtils.hasText(userMessage)) {
            throw new IllegalArgumentException("message 不能为空");
        }
        ensureAssistantInitialized();
        chatCandidateContext.clear();
        try {
            log.info("HR agent stream chat, sessionId={}, messageLength={}", sessionId, userMessage.length());
            StringBuilder replyBuilder = new StringBuilder();
            TokenStream stream = hrAssistantStream.chat(sessionId, userMessage.trim());
            stream.onNext(token -> {
                        replyBuilder.append(token);
                        onToken.accept(token);
                    })
                    .onComplete(response -> {
                        try {
                            String reply = replyBuilder.toString();
                            if (!StringUtils.hasText(reply)
                                    && response != null
                                    && response.content() != null
                                    && response.content().text() != null) {
                                reply = response.content().text();
                            }
                            onComplete.accept(new AgentChatOutcome(reply, chatCandidateContext.snapshot()));
                        } finally {
                            chatCandidateContext.remove();
                        }
                    })
                    .onError(error -> {
                        try {
                            if (error instanceof AgentBusinessException businessException) {
                                onError.accept(businessException);
                                return;
                            }
                            log.error("HR agent stream chat failed, sessionId={}", sessionId, error);
                            onError.accept(new AgentBusinessException("大模型调用失败：" + error.getMessage(), error));
                        } finally {
                            chatCandidateContext.remove();
                        }
                    })
                    .start();
        } catch (AgentBusinessException e) {
            chatCandidateContext.remove();
            throw e;
        } catch (Exception e) {
            chatCandidateContext.remove();
            log.error("HR agent stream chat failed, sessionId={}", sessionId, e);
            throw new AgentBusinessException("大模型调用失败：" + e.getMessage(), e);
        }
    }

    private ChatLanguageModel buildChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(dashScopeProperties.getBaseUrl())
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModel())
                .timeout(Duration.ofSeconds(dashScopeProperties.getTimeoutSeconds()))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private StreamingChatLanguageModel buildStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(dashScopeProperties.getBaseUrl())
                .apiKey(dashScopeProperties.getApiKey())
                .modelName(dashScopeProperties.getModel())
                .timeout(Duration.ofSeconds(dashScopeProperties.getTimeoutSeconds()))
                .build();
    }
}
