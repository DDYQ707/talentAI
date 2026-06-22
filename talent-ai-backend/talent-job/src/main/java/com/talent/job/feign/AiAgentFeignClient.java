package com.talent.job.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI 画像微服务 Feign 客户端（内部调用）
 * <p>用于候选人状态变更时触发 AI 画像生成</p>
 */
@FeignClient(name = "talent-ai-agent")
public interface AiAgentFeignClient {

    /**
     * 触发 AI 人才画像生成（异步/非阻塞，失败可忽略）
     */
    @PostMapping("/api/ai/profile/generate")
    Map<String, Object> generateProfile(@RequestBody Map<String, Object> body);
}
