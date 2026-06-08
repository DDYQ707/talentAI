package com.talent.agent.service;

import com.talent.agent.domain.dto.LlmMatchOutcome;
import com.talent.agent.domain.dto.MatchRequest;

public interface MatchResultPersistService {

    void saveSuccess(Long matchId, MatchRequest request, LlmMatchOutcome outcome);
}
