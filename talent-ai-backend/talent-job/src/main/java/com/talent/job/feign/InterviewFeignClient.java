package com.talent.job.feign;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "talent-interview", contextId = "jobInterviewFeignClient")
public interface InterviewFeignClient {

    /** HR 淘汰候选人后联动：取消该投递下待面试/待安排记录 */
    @PostMapping("/api/interview/internal/application/{applicationId}/reject-sync")
    Map<String, Object> syncApplicationRejected(@PathVariable("applicationId") Long applicationId);
}
