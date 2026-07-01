package com.talent.agent.service;

import com.talent.agent.domain.dto.AiMatchTriggerRequest;
import com.talent.agent.domain.dto.MatchRequest;
import com.talent.agent.domain.vo.MatchResultVO;

public interface AiMatchService {

    MatchResultVO submitMatch(MatchRequest request);

    /** HR 手动触发人岗匹配（需简历已解析完成） */
    MatchResultVO triggerMatchForHr(AiMatchTriggerRequest request);

    MatchResultVO getByApplicationId(Long applicationId);

    MatchResultVO getLatestByResumeAndJob(Long resumeId, Long jobId);
}
