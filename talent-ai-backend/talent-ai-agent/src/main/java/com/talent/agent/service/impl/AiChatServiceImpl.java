package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.chat.agent.AgentChatOutcome;
import com.talent.agent.chat.agent.HrRecruitAgentService;
import com.talent.agent.config.DashScopeProperties;
import com.talent.agent.constant.ChatConstants;
import com.talent.agent.domain.dto.ChatSendRequest;
import com.talent.agent.domain.entity.AiChatMessage;
import com.talent.agent.domain.entity.AiChatSession;
import com.talent.agent.domain.vo.ChatMessageVO;
import com.talent.agent.domain.vo.ChatSendResponseVO;
import com.talent.agent.domain.vo.ChatSessionVO;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.mapper.AiChatMessageMapper;
import com.talent.agent.mapper.AiChatSessionMapper;
import com.talent.agent.service.AiChatService;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final HrRecruitAgentService hrRecruitAgentService;
    private final TransactionTemplate transactionTemplate;
    private final DashScopeProperties dashScopeProperties;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSendResponseVO sendMessage(Long userId, ChatSendRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("message 不能为空");
        }
        String userMessage = request.getMessage().trim();
        if (userMessage.length() > 4000) {
            throw new IllegalArgumentException("消息过长，请控制在 4000 字以内");
        }

        AiChatSession session = resolveSession(userId, request.getSessionId(), userMessage);

        AgentChatOutcome outcome = hrRecruitAgentService.chat(session.getId(), userMessage);
        String reply = outcome.getReply();
        insertMessage(session.getId(), ChatConstants.ROLE_USER, userMessage);
        insertMessage(session.getId(), ChatConstants.ROLE_ASSISTANT, reply);
        touchSession(session);

        ChatSendResponseVO vo = new ChatSendResponseVO();
        vo.setSessionId(session.getId());
        vo.setSessionTitle(session.getSessionTitle());
        vo.setReply(reply);
        vo.setCandidates(outcome.getCandidates());
        return vo;
    }

    @Override
    public SseEmitter streamMessage(Long userId, ChatSendRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("message 不能为空");
        }
        String userMessage = request.getMessage().trim();
        if (userMessage.length() > 4000) {
            throw new IllegalArgumentException("消息过长，请控制在 4000 字以内");
        }

        AiChatSession session = transactionTemplate.execute(
                status -> resolveSession(userId, request.getSessionId(), userMessage));

        long timeoutMs = Duration.ofSeconds(dashScopeProperties.getTimeoutSeconds() + 60L).toMillis();
        SseEmitter emitter = new SseEmitter(timeoutMs);

        try {
            emitter.send(SseEmitter.event()
                    .name("meta")
                    .data(Map.of(
                            "sessionId", session.getId(),
                            "sessionTitle", session.getSessionTitle())));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> log.warn("SSE connection error, sessionId={}", session.getId(), ex));

        try {
            hrRecruitAgentService.streamChat(
                    session.getId(),
                    userMessage,
                    token -> sendToken(emitter, token),
                    outcome -> completeStream(emitter, session, userMessage, outcome),
                    error -> failStream(emitter, error));
        } catch (Exception e) {
            failStream(emitter, e);
        }

        return emitter;
    }

    private void sendToken(SseEmitter emitter, String token) {
        try {
            emitter.send(SseEmitter.event().name("token").data(Map.of("text", token)));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void completeStream(
            SseEmitter emitter, AiChatSession session, String userMessage, AgentChatOutcome outcome) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                insertMessage(session.getId(), ChatConstants.ROLE_USER, userMessage);
                insertMessage(session.getId(), ChatConstants.ROLE_ASSISTANT, outcome.getReply());
                touchSession(session);
            });

            ChatSendResponseVO vo = new ChatSendResponseVO();
            vo.setSessionId(session.getId());
            vo.setSessionTitle(session.getSessionTitle());
            vo.setReply(outcome.getReply());
            vo.setCandidates(outcome.getCandidates());

            emitter.send(SseEmitter.event().name("done").data(vo));
            emitter.complete();
        } catch (Exception e) {
            failStream(emitter, e);
        }
    }

    private void failStream(SseEmitter emitter, Throwable error) {
        try {
            String message = error instanceof AgentBusinessException businessException
                    ? businessException.getMessage()
                    : (error.getMessage() != null ? error.getMessage() : "流式回复失败");
            emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
            emitter.complete();
        } catch (IOException sendError) {
            log.warn("Failed to send SSE error event", sendError);
            emitter.completeWithError(error);
        }
    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId) {
        List<AiChatSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<AiChatSession>()
                        .eq(AiChatSession::getUserId, userId)
                        .orderByDesc(AiChatSession::getUpdatedAt)
                        .last("LIMIT 50"));
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatSessionVO> result = new ArrayList<>(sessions.size());
        for (AiChatSession session : sessions) {
            result.add(toSessionVO(session));
        }
        return result;
    }

    @Override
    public List<ChatMessageVO> listMessages(Long userId, Long sessionId) {
        AiChatSession session = requireOwnedSession(userId, sessionId);
        List<AiChatMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, session.getId())
                        .orderByAsc(AiChatMessage::getCreatedAt));
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatMessageVO> result = new ArrayList<>(messages.size());
        for (AiChatMessage message : messages) {
            result.add(toMessageVO(message));
        }
        return result;
    }

    private AiChatSession resolveSession(Long userId, Long sessionId, String firstMessage) {
        if (sessionId != null && sessionId > 0) {
            return requireOwnedSession(userId, sessionId);
        }
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setSessionTitle(buildSessionTitle(firstMessage));
        sessionMapper.insert(session);
        return session;
    }

    private AiChatSession requireOwnedSession(Long userId, Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new IllegalArgumentException("sessionId 无效");
        }
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new IllegalArgumentException("无权访问该会话");
        }
        return session;
    }

    private void insertMessage(Long sessionId, int role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        messageMapper.insert(message);
    }

    private void touchSession(AiChatSession session) {
        AiChatSession touch = new AiChatSession();
        touch.setId(session.getId());
        touch.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(touch);
    }

    private String buildSessionTitle(String message) {
        String normalized = message.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= ChatConstants.SESSION_TITLE_MAX_LEN) {
            return normalized;
        }
        return normalized.substring(0, ChatConstants.SESSION_TITLE_MAX_LEN) + "...";
    }

    private ChatSessionVO toSessionVO(AiChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setSessionTitle(session.getSessionTitle());
        vo.setCreatedAt(session.getCreatedAt());
        vo.setUpdatedAt(session.getUpdatedAt());
        return vo;
    }

    private ChatMessageVO toMessageVO(AiChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }
}
