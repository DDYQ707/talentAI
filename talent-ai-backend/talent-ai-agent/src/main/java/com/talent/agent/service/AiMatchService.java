package com.talent.agent.service;

import com.talent.agent.domain.dto.MatchRequest;
import com.talent.agent.domain.vo.MatchResultVO;

public interface AiMatchService {

    MatchResultVO submitMatch(MatchRequest request);

    MatchResultVO getByApplicationId(Long applicationId);

    MatchResultVO getLatestByResumeAndJob(Long resumeId, Long jobId);
}
