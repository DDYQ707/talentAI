package com.talent.analytics.feign.fallback;

import com.talent.analytics.feign.ResumeFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResumeFeignFallback implements ResumeFeignClient {

    @Override
    public Long getTotalResumeCount() {
        log.warn("[Sentinel] ResumeFeign.getTotalResumeCount 降级");
        return 0L;
    }
}
