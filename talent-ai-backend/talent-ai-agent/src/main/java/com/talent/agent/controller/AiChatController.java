package com.talent.agent.controller;

import com.talent.agent.domain.dto.ChatSendRequest;
import com.talent.agent.domain.vo.ChatMessageVO;
import com.talent.agent.domain.vo.ChatSendResponseVO;
import com.talent.agent.domain.vo.ChatSessionVO;
import com.talent.agent.service.AiChatService;
import com.talent.agent.util.ChatAuthSupport;
import com.talent.common.api.R;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping
    public R<ChatSendResponseVO> send(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody ChatSendRequest request) {
        ChatAuthSupport.requireUserId(userId);
        ChatAuthSupport.requireHr(role);
        return R.ok(aiChatService.sendMessage(userId, request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody ChatSendRequest request) {
        ChatAuthSupport.requireUserId(userId);
        ChatAuthSupport.requireHr(role);
        return aiChatService.streamMessage(userId, request);
    }

    @GetMapping("/sessions")
    public R<List<ChatSessionVO>> sessions(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        ChatAuthSupport.requireUserId(userId);
        ChatAuthSupport.requireHr(role);
        return R.ok(aiChatService.listSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public R<List<ChatMessageVO>> messages(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long sessionId) {
        ChatAuthSupport.requireUserId(userId);
        ChatAuthSupport.requireHr(role);
        return R.ok(aiChatService.listMessages(userId, sessionId));
    }
}
