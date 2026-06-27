package com.talent.interview.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "talent-ai-agent", contextId = "interviewAiProfileFeignClient")
public interface AiAgentFeignClient {

    /** 面试完成后触发 AI 人才画像生成（失败可忽略） */
    @PostMapping("/api/ai/profile/generate")
    Map<String, Object> generateProfile(@RequestBody Map<String, Object> body);
}
