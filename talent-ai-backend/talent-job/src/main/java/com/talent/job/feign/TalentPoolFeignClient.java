package com.talent.job.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 人才库微服务 Feign 客户端（内部调用）
 * <p>用于候选人淘汰时自动归档至人才库</p>
 */
@FeignClient(name = "talent-talent-pool")
public interface TalentPoolFeignClient {

    /**
     * 内部调用：将候选人归档至人才库
     */
    @PostMapping("/api/talent-pool/internal/archive")
    Map<String, Object> archiveToPool(@RequestBody Map<String, Object> body);
}
