package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.client.AuthFeignClient;
import com.talent.agent.client.ResumeFeignClient;
import com.talent.agent.domain.dto.LlmResumeQualityOutcome;
import com.talent.agent.domain.dto.ParsedResumeQualityDto;
import com.talent.agent.domain.dto.ResumeQualityEvaluateRequest;
import com.talent.agent.domain.entity.AiModel;
import com.talent.agent.domain.entity.AiParseTask;
import com.talent.agent.domain.entity.AiResumeParseResult;
import com.talent.agent.domain.entity.AiResumeQualityScore;
import com.talent.agent.domain.vo.ResumeQualityScoreVO;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.mapper.AiModelMapper;
import com.talent.agent.mapper.AiParseTaskMapper;
import com.talent.agent.mapper.AiResumeParseResultMapper;
import com.talent.agent.mapper.AiResumeQualityScoreMapper;
import com.talent.agent.service.AiResumeQualityService;
import com.talent.agent.service.ResumeQualityLlmService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiResumeQualityServiceImpl implements AiResumeQualityService {

    private static final int PARSE_STATUS_SUCCESS = 2;
    private static final String DEFAULT_MODEL_CODE = "qwen-max";

    private final AiResumeQualityScoreMapper qualityScoreMapper;
    private final AiResumeParseResultMapper parseResultMapper;
    private final AiParseTaskMapper parseTaskMapper;
    private final AiModelMapper modelMapper;
    private final ResumeFeignClient resumeFeignClient;
    private final AuthFeignClient authFeignClient;
    private final ResumeQualityLlmService resumeQualityLlmService;
    private final ObjectMapper objectMapper;

    @Override
    public ResumeQualityScoreVO getLatestByResumeId(Long resumeId) {
        if (resumeId == null) {
            throw new IllegalArgumentException("resumeId 不能为空");
        }
        AiResumeQualityScore entity = qualityScoreMapper.selectOne(
                new LambdaQueryWrapper<AiResumeQualityScore>()
                        .eq(AiResumeQualityScore::getResumeId, resumeId)
                        .orderByDesc(AiResumeQualityScore::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        return entity == null ? null : toVO(entity);
    }

    @Override
    @Transactional
    public ResumeQualityScoreVO evaluate(Long operatorUserId, ResumeQualityEvaluateRequest request) {
        if (operatorUserId == null) {
            throw new IllegalArgumentException("未登录或用户信息缺失");
        }
        if (request == null || request.getResumeId() == null) {
            throw new IllegalArgumentException("resumeId 不能为空");
        }

        ResolvedResume resolved = resolveResume(request.getResumeId());
        assertCandidateOwnership(operatorUserId, resolved.candidateId());

        ParseContext parseCtx = loadSuccessfulParseContext(resolved.resumeId());
        boolean forceRefresh = Boolean.TRUE.equals(request.getForceRefresh());

        if (!forceRefresh && parseCtx.taskId() != null) {
            AiResumeQualityScore cached = qualityScoreMapper.selectOne(
                    new LambdaQueryWrapper<AiResumeQualityScore>()
                            .eq(AiResumeQualityScore::getResumeId, resolved.resumeId())
                            .eq(AiResumeQualityScore::getParseTaskId, parseCtx.taskId())
                            .orderByDesc(AiResumeQualityScore::getCreatedAt)
                            .last("LIMIT 1"),
                    false);
            if (cached != null) {
                return toVO(cached);
            }
        }

        LlmResumeQualityOutcome outcome = resumeQualityLlmService.evaluate(parseCtx.parsedJson());
        AiResumeQualityScore saved = persistScore(
                resolved.resumeId(),
                resolved.candidateId(),
                parseCtx.taskId(),
                outcome);
        syncCandidateAiScore(resolved.candidateId(), saved.getQualityScore());

        log.info(
                "简历质量评分完成 resumeId={} candidateId={} score={}",
                resolved.resumeId(),
                resolved.candidateId(),
                saved.getQualityScore());
        return toVO(saved);
    }

    @Async
    @Override
    public void evaluateAfterParseAsync(Long resumeId, Long candidateId, Long parseTaskId) {
        if (resumeId == null || candidateId == null) {
            return;
        }
        try {
            ResumeQualityEvaluateRequest request = new ResumeQualityEvaluateRequest();
            request.setResumeId(resumeId);
            request.setForceRefresh(true);
            evaluate(candidateId, request);
        } catch (Exception e) {
            log.warn(
                    "解析后自动简历质量评分失败 resumeId={} parseTaskId={} reason={}",
                    resumeId,
                    parseTaskId,
                    e.getMessage());
        }
    }

    private record ResolvedResume(Long resumeId, Long candidateId) {}

    private record ParseContext(Long taskId, String parsedJson) {}

    private ResolvedResume resolveResume(Long resumeId) {
        Map<String, Object> response = resumeFeignClient.getResumeOwnership(resumeId);
        if (response == null || !Boolean.TRUE.equals(response.get("valid"))) {
            throw new AgentBusinessException("简历不存在或无效");
        }
        Object candidateIdObj = response.get("candidateId");
        if (!(candidateIdObj instanceof Number candidateNum)) {
            throw new AgentBusinessException("简历归属信息异常");
        }
        Long primaryResumeId = resumeId;
        Object primaryObj = response.get("primaryResumeId");
        if (primaryObj instanceof Number primaryNum) {
            primaryResumeId = primaryNum.longValue();
        }
        return new ResolvedResume(primaryResumeId, candidateNum.longValue());
    }

    private void assertCandidateOwnership(Long operatorUserId, Long candidateId) {
        if (!operatorUserId.equals(candidateId)) {
            throw new AgentBusinessException("无权评价该简历");
        }
    }

    private ParseContext loadSuccessfulParseContext(Long resumeId) {
        AiParseTask task = parseTaskMapper.selectOne(
                new LambdaQueryWrapper<AiParseTask>()
                        .eq(AiParseTask::getResumeId, resumeId)
                        .eq(AiParseTask::getTaskStatus, PARSE_STATUS_SUCCESS)
                        .orderByDesc(AiParseTask::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        if (task == null) {
            throw new AgentBusinessException("暂无成功的简历解析结果，请先完善简历并投递或等待解析完成");
        }

        AiResumeParseResult parseResult = parseResultMapper.selectOne(
                new LambdaQueryWrapper<AiResumeParseResult>()
                        .eq(AiResumeParseResult::getTaskId, task.getId())
                        .last("LIMIT 1"),
                false);
        if (parseResult == null || !StringUtils.hasText(parseResult.getParsedJson())) {
            throw new AgentBusinessException("简历解析结果为空，请稍后重试");
        }
        return new ParseContext(task.getId(), parseResult.getParsedJson());
    }

    private AiResumeQualityScore persistScore(
            Long resumeId, Long candidateId, Long parseTaskId, LlmResumeQualityOutcome outcome) {
        ParsedResumeQualityDto quality = outcome.quality();
        AiResumeQualityScore entity = new AiResumeQualityScore();
        entity.setResumeId(resumeId);
        entity.setCandidateId(candidateId);
        entity.setParseTaskId(parseTaskId);
        entity.setQualityScore(quality.getQualityScore());
        entity.setSummary(quality.getSummary());
        entity.setStrengths(toJson(quality.getStrengths()));
        entity.setWeaknesses(toJson(quality.getWeaknesses()));
        entity.setSuggestions(toJson(quality.getSuggestions()));
        entity.setDimensionScores(toJson(quality.getDimensionScores()));
        entity.setModelId(resolveModelId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        qualityScoreMapper.insert(entity);
        return entity;
    }

    private Long resolveModelId() {
        AiModel model = modelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getModelCode, DEFAULT_MODEL_CODE)
                        .last("LIMIT 1"),
                false);
        return model != null ? model.getId() : null;
    }

    private void syncCandidateAiScore(Long candidateId, Integer qualityScore) {
        if (candidateId == null || qualityScore == null) {
            return;
        }
        try {
            authFeignClient.syncCandidateAiScore(candidateId, qualityScore);
        } catch (Exception e) {
            log.warn("同步候选人 AI 评分失败 candidateId={} score={} reason={}", candidateId, qualityScore, e.getMessage());
        }
    }

    private ResumeQualityScoreVO toVO(AiResumeQualityScore entity) {
        ResumeQualityScoreVO vo = new ResumeQualityScoreVO();
        vo.setScoreId(entity.getId());
        vo.setResumeId(entity.getResumeId());
        vo.setCandidateId(entity.getCandidateId());
        vo.setParseTaskId(entity.getParseTaskId());
        vo.setQualityScore(entity.getQualityScore());
        vo.setSummary(entity.getSummary());
        vo.setStrengths(parseStringList(entity.getStrengths()));
        vo.setWeaknesses(parseStringList(entity.getWeaknesses()));
        vo.setSuggestions(parseStringList(entity.getSuggestions()));
        vo.setDimensionScores(parseDimensionMap(entity.getDimensionScores()));
        vo.setEvaluatedAt(entity.getCreatedAt());
        return vo;
    }

    private List<String> parseStringList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Integer> parseDimensionMap(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new AgentBusinessException("评分结果序列化失败：" + e.getMessage(), e);
        }
    }
}
