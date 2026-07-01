package com.talent.job.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "talent-talent-pool")
public interface TalentPoolFeignClient {

    @PostMapping("/talent-pool/internal/archive")
    Map<String, Object> archive(@RequestBody Map<String, Object> body);
}
