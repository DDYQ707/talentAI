package com.talent.job.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "talent-ai-agent", contextId = "aiParseFeignClient")
public interface AiFeignClient {

    @PostMapping("/internal/ai/parse/submit")
    Map<String, Object> submitParse(@RequestBody Map<String, Object> body);
}
