package com.talent.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.client.JobFeignClient;
import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.LlmMatchOutcome;
import com.talent.agent.domain.dto.MatchRequest;
import com.talent.agent.domain.dto.ParsedMatchDto;
import com.talent.agent.domain.entity.AiMatchRecord;
import com.talent.agent.domain.entity.AiModel;
import com.talent.agent.domain.entity.AiResumeParseResult;
import com.talent.agent.mapper.AiMatchRecordMapper;
import com.talent.agent.mapper.AiModelMapper;
import com.talent.agent.mapper.AiResumeParseResultMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMatchTaskProcessor {

    private static final int STATUS_PROCESSING = 1;
    private static final int STATUS_SUCCESS = 2;
    private static final int STATUS_FAILED = 3;

    private static final int MAX_ERROR_LENGTH = 500;
    private static final String DEFAULT_MODEL_CODE = "qwen-max";

    private final AiMatchRecordMapper matchRecordMapper;
    private final AiResumeParseResultMapper parseResultMapper;
    private final AiModelMapper modelMapper;
    private final JobBriefQueryService jobBriefQueryService;
    private final ResumeJobMatchService resumeJobMatchService;
    private final JobFeignClient jobFeignClient;
    private final MatchResultPersistService matchResultPersistService;

    @Async
    public void processAsync(Long matchId, MatchRequest request) {
        markProcessing(matchId);
        try {
            AiResumeParseResult parseResult = loadParseResult(request);
            JobBriefInfo job = jobBriefQueryService.fetchJobBrief(request.getJobId());
            LlmMatchOutcome outcome = resumeJobMatchService.match(job, parseResult.getParsedJson());
            matchResultPersistService.saveSuccess(matchId, request, outcome);
            syncApplicationMatchScore(request.getApplicationId(), outcome.match().getMatchScore());
            log.info(
                    "人岗匹配成功 matchId={} applicationId={} jobId={} resumeId={} score={}",
                    matchId,
                    request.getApplicationId(),
                    request.getJobId(),
                    request.getResumeId(),
                    outcome.match().getMatchScore());
        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            markFailed(matchId, reason);
            log.warn(
                    "人岗匹配失败 matchId={} applicationId={} jobId={} resumeId={} reason={}",
                    matchId,
                    request.getApplicationId(),
                    request.getJobId(),
                    request.getResumeId(),
                    truncate(reason));
        }
    }

    private AiResumeParseResult loadParseResult(MatchRequest request) {
        if (request.getParseTaskId() != null) {
            AiResumeParseResult byTask = parseResultMapper.selectOne(
                    new LambdaQueryWrapper<AiResumeParseResult>()
                            .eq(AiResumeParseResult::getTaskId, request.getParseTaskId())
                            .last("LIMIT 1"),
                    false);
            if (byTask != null && StringUtils.hasText(byTask.getParsedJson())) {
                return byTask;
            }
        }
        AiResumeParseResult latest = parseResultMapper.selectOne(
                new LambdaQueryWrapper<AiResumeParseResult>()
                        .eq(AiResumeParseResult::getResumeId, request.getResumeId())
                        .orderByDesc(AiResumeParseResult::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        if (latest == null || !StringUtils.hasText(latest.getParsedJson())) {
            throw new IllegalStateException("未找到可用的简历结构化解析结果");
        }
        return latest;
    }

    private void markProcessing(Long matchId) {
        AiMatchRecord update = new AiMatchRecord();
        update.setId(matchId);
        update.setMatchStatus(STATUS_PROCESSING);
        update.setStartedAt(LocalDateTime.now());
        update.setModelId(resolveModelId(null));
        matchRecordMapper.updateById(update);
    }

    private void markFailed(Long matchId, String reason) {
        AiMatchRecord update = new AiMatchRecord();
        update.setId(matchId);
        update.setMatchStatus(STATUS_FAILED);
        update.setErrorMessage(truncate(reason));
        update.setFinishedAt(LocalDateTime.now());
        matchRecordMapper.updateById(update);
    }

    private void syncApplicationMatchScore(Long applicationId, Integer matchScore) {
        if (applicationId == null || matchScore == null) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("applicationId", applicationId);
            body.put("matchScore", matchScore);
            jobFeignClient.syncApplicationMatchScore(body);
        } catch (Exception e) {
            log.warn("同步投递单匹配分失败 applicationId={} reason={}", applicationId, e.getMessage());
        }
    }

    private Long resolveModelId(Long requestModelId) {
        if (requestModelId != null) {
            return requestModelId;
        }
        AiModel model = modelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>().eq(AiModel::getModelCode, DEFAULT_MODEL_CODE).last("LIMIT 1"),
                false);
        return model != null ? model.getId() : null;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
