package com.talent.analytics.feign.fallback;

import com.talent.analytics.feign.InterviewFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class InterviewFeignFallback implements InterviewFeignClient {

    @Override
    public Long countOngoingInterviews() {
        log.warn("[Sentinel] InterviewFeign.countOngoingInterviews 降级");
        return 0L;
    }

    @Override
    public Long countCompletedThisMonth() {
        log.warn("[Sentinel] InterviewFeign.countCompletedThisMonth 降级");
        return 0L;
    }

    @Override
    public Long countPassedInterviews(String yearMonth) {
        log.warn("[Sentinel] InterviewFeign.countPassedInterviews 降级");
        return 0L;
    }

    @Override
    public Map<String, Long> countMonthlyCompleted(int months) {
        log.warn("[Sentinel] InterviewFeign.countMonthlyCompleted 降级");
        return Collections.emptyMap();
    }
}
