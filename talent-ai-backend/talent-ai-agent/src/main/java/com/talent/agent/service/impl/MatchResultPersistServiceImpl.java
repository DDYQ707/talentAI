package com.talent.agent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.LlmMatchOutcome;
import com.talent.agent.domain.dto.MatchRequest;
import com.talent.agent.domain.dto.ParsedMatchDto;
import com.talent.agent.domain.entity.AiMatchRecord;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.mapper.AiMatchRecordMapper;
import com.talent.agent.service.MatchResultPersistService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchResultPersistServiceImpl implements MatchResultPersistService {

    private static final int STATUS_SUCCESS = 2;

    private final AiMatchRecordMapper matchRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveSuccess(Long matchId, MatchRequest request, LlmMatchOutcome outcome) {
        ParsedMatchDto match = outcome.match();
        AiMatchRecord update = new AiMatchRecord();
        update.setId(matchId);
        update.setApplicationId(request.getApplicationId());
        update.setJobId(request.getJobId());
        update.setResumeId(request.getResumeId());
        update.setMatchScore(match.getMatchScore() != null ? match.getMatchScore() : 0);
        update.setMatchStatus(STATUS_SUCCESS);
        update.setMatchLevel(match.getMatchLevel());
        update.setMatchReason(match.getMatchReason());
        update.setAdvantages(toJson(match.getAdvantages()));
        update.setDisadvantages(toJson(match.getDisadvantages()));
        update.setSuggestedQuestions(toJson(match.getSuggestedQuestions()));
        update.setDimensionScores(toJson(match.getDimensionScores()));
        update.setErrorMessage(null);
        update.setFinishedAt(LocalDateTime.now());
        matchRecordMapper.updateById(update);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new AgentBusinessException("匹配结果序列化失败：" + e.getMessage(), e);
        }
    }
}
