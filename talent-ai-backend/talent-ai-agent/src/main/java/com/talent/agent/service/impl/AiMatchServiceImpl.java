package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.domain.dto.MatchRequest;
import com.talent.agent.domain.entity.AiMatchRecord;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.mapper.AiMatchRecordMapper;
import com.talent.agent.service.AiMatchService;
import com.talent.agent.service.AiMatchTaskProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiMatchServiceImpl implements AiMatchService {

    private static final int MATCH_STATUS_PENDING = 0;

    private final AiMatchRecordMapper matchRecordMapper;
    private final AiMatchTaskProcessor matchTaskProcessor;

    @Override
    @Transactional
    public MatchResultVO submitMatch(MatchRequest request) {
        if (request == null || request.getJobId() == null || request.getResumeId() == null) {
            throw new IllegalArgumentException("jobId、resumeId 不能为空");
        }
        AiMatchRecord record = new AiMatchRecord();
        record.setApplicationId(request.getApplicationId());
        record.setJobId(request.getJobId());
        record.setResumeId(request.getResumeId());
        record.setModelId(request.getModelId());
        record.setMatchScore(0);
        record.setMatchStatus(MATCH_STATUS_PENDING);
        record.setTokenUsed(0);
        matchRecordMapper.insert(record);

        matchTaskProcessor.processAsync(record.getId(), request);
        return toVO(record);
    }

    @Override
    public MatchResultVO getByApplicationId(Long applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("applicationId 不能为空");
        }
        AiMatchRecord record = matchRecordMapper.selectOne(
                new LambdaQueryWrapper<AiMatchRecord>()
                        .eq(AiMatchRecord::getApplicationId, applicationId)
                        .orderByDesc(AiMatchRecord::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        return record == null ? null : toVO(record);
    }

    @Override
    public MatchResultVO getLatestByResumeAndJob(Long resumeId, Long jobId) {
        if (resumeId == null || jobId == null) {
            throw new IllegalArgumentException("resumeId 与 jobId 不能为空");
        }
        AiMatchRecord record = matchRecordMapper.selectOne(
                new LambdaQueryWrapper<AiMatchRecord>()
                        .eq(AiMatchRecord::getResumeId, resumeId)
                        .eq(AiMatchRecord::getJobId, jobId)
                        .orderByDesc(AiMatchRecord::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        return record == null ? null : toVO(record);
    }

    private MatchResultVO toVO(AiMatchRecord record) {
        MatchResultVO vo = new MatchResultVO();
        vo.setMatchId(record.getId());
        vo.setApplicationId(record.getApplicationId());
        vo.setJobId(record.getJobId());
        vo.setResumeId(record.getResumeId());
        vo.setMatchScore(record.getMatchScore());
        vo.setMatchStatus(record.getMatchStatus());
        vo.setMatchLevel(record.getMatchLevel());
        vo.setMatchReason(record.getMatchReason());
        vo.setAdvantages(record.getAdvantages());
        vo.setDisadvantages(record.getDisadvantages());
        vo.setSuggestedQuestions(record.getSuggestedQuestions());
        vo.setDimensionScores(record.getDimensionScores());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setStartedAt(record.getStartedAt());
        vo.setFinishedAt(record.getFinishedAt());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
