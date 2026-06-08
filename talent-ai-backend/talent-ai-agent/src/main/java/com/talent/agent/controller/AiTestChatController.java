package com.talent.agent.controller;

import com.talent.agent.domain.dto.ChatTestRequest;
import com.talent.agent.domain.vo.ChatTestResponseVO;
import com.talent.agent.service.LlmChatService;
import com.talent.common.api.R;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大模型连通性测试接口（仅用于 Sprint1 验证）
 */
@RestController
@RequestMapping("/api/ai/test")
@RequiredArgsConstructor
public class AiTestChatController {

    private static final String TEST_SYSTEM_PROMPT =
            "你是连通性测试助手。请严格按用户指令回复，不要添加额外说明、标点或换行。";

    private final LlmChatService llmChatService;

    @PostMapping("/chat")
    public R<ChatTestResponseVO> testChat(@RequestBody ChatTestRequest request) {
        if (request == null || !StringUtils.hasText(request.getPrompt())) {
            throw new IllegalArgumentException("prompt 不能为空");
        }

        String content = llmChatService.chat(TEST_SYSTEM_PROMPT, request.getPrompt());

        ChatTestResponseVO vo = new ChatTestResponseVO();
        vo.setContent(content);
        return R.ok(vo);
    }
}
