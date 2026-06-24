package com.talent.agent.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-auth")
public interface AuthFeignClient {

    /** 同步候选人档案 AI 简历评分 */
    @PutMapping("/api/auth/candidate/profile/internal/ai-score")
    java.util.Map<String, Object> syncCandidateAiScore(
            @RequestParam("userId") Long userId,
            @RequestParam("aiScore") Integer aiScore);
}
