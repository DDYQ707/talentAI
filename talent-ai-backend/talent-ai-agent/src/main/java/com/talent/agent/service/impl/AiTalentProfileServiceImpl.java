package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.client.InterviewFeignClient;
import com.talent.agent.client.JobFeignClient;
import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.ParsedTalentProfileDto;
import com.talent.agent.domain.dto.ProfileGenerateRequest;
import com.talent.agent.domain.entity.AiModel;
import com.talent.agent.domain.entity.AiResumeParseResult;
import com.talent.agent.domain.entity.AiTalentProfile;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.domain.vo.TalentProfileVO;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.mapper.AiModelMapper;
import com.talent.agent.mapper.AiResumeParseResultMapper;
import com.talent.agent.mapper.AiTalentProfileMapper;
import com.talent.agent.service.AiMatchService;
import com.talent.agent.service.AiTalentProfileService;
import com.talent.agent.service.JobBriefQueryService;
import com.talent.agent.service.TalentProfileLlmService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTalentProfileServiceImpl implements AiTalentProfileService {

    private static final String DEFAULT_MODEL_CODE = "qwen-max";

    private final AiTalentProfileMapper profileMapper;
    private final AiResumeParseResultMapper parseResultMapper;
    private final AiModelMapper modelMapper;
    private final JobFeignClient jobFeignClient;
    private final InterviewFeignClient interviewFeignClient;
    private final JobBriefQueryService jobBriefQueryService;
    private final AiMatchService aiMatchService;
    private final TalentProfileLlmService talentProfileLlmService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TalentProfileVO generate(ProfileGenerateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        ProfileContext ctx = resolveContext(request);

        AiResumeParseResult parseResult = loadParseResult(ctx.resumeId());
        JobBriefInfo job = jobBriefQueryService.fetchJobBrief(ctx.jobId());
        MatchResultVO matchResult = aiMatchService.getByApplicationId(ctx.applicationId());
        List<Map<String, Object>> evaluations = loadInterviewEvaluations(ctx.applicationId());

        ParsedTalentProfileDto llmResult = talentProfileLlmService.generate(
                job,
                parseResult.getParsedJson(),
                matchResult,
                evaluations,
                ctx.candidateName(),
                ctx.applicationStatus());

        AiTalentProfile saved = saveProfile(ctx, llmResult);
        log.info(
                "人才画像生成成功 profileId={} applicationId={} candidateId={} version={}",
                saved.getId(),
                ctx.applicationId(),
                ctx.candidateId(),
                saved.getVersion());
        return toVO(saved);
    }

    @Override
    public TalentProfileVO getByApplicationId(Long applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("applicationId 不能为空");
        }
        AiTalentProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<AiTalentProfile>()
                        .eq(AiTalentProfile::getApplicationId, applicationId)
                        .orderByDesc(AiTalentProfile::getVersion)
                        .orderByDesc(AiTalentProfile::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        return profile == null ? null : toVO(profile);
    }

    private ProfileContext resolveContext(ProfileGenerateRequest request) {
        if (request.getApplicationId() != null) {
            return enrichFromApplication(request);
        }
        if (request.getResumeId() != null && request.getJobId() != null) {
            return new ProfileContext(
                    request.getCandidateId(),
                    request.getCandidateName(),
                    request.getResumeId(),
                    null,
                    request.getJobId(),
                    request.getJobTitle(),
                    request.getStatus());
        }
        throw new IllegalArgumentException("applicationId 不能为空");
    }

    private ProfileContext enrichFromApplication(ProfileGenerateRequest request) {
        Long applicationId = request.getApplicationId();
        Map<String, Object> appResponse = jobFeignClient.getApplicationById(applicationId);
        Map<String, Object> appData = extractDataMap(appResponse);
        if (appData == null) {
            throw new AgentBusinessException("投递记录不存在: " + applicationId);
        }

        Long candidateId = firstLong(request.getCandidateId(), appData.get("candidateId"));
        Long resumeId = firstLong(request.getResumeId(), appData.get("resumeId"));
        Long jobId = firstLong(request.getJobId(), appData.get("jobId"));
        if (resumeId == null || jobId == null) {
            throw new AgentBusinessException("投递记录缺少 resumeId 或 jobId");
        }

        String candidateName = firstString(request.getCandidateName(), appData.get("candidateName"));
        String jobTitle = firstString(request.getJobTitle(), appData.get("jobTitle"));
        Integer status = request.getStatus() != null ? request.getStatus() : intVal(appData.get("status"));

        return new ProfileContext(candidateId, candidateName, resumeId, applicationId, jobId, jobTitle, status);
    }

    private AiResumeParseResult loadParseResult(Long resumeId) {
        AiResumeParseResult latest = parseResultMapper.selectOne(
                new LambdaQueryWrapper<AiResumeParseResult>()
                        .eq(AiResumeParseResult::getResumeId, resumeId)
                        .orderByDesc(AiResumeParseResult::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        if (latest == null || !StringUtils.hasText(latest.getParsedJson())) {
            throw new AgentBusinessException("未找到可用的简历结构化解析结果，请等待简历解析完成");
        }
        return latest;
    }

    private List<Map<String, Object>> loadInterviewEvaluations(Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }
        Map<String, Object> response = interviewFeignClient.listEvaluationsByApplication(applicationId);
        return extractDataList(response);
    }

    private AiTalentProfile saveProfile(ProfileContext ctx, ParsedTalentProfileDto llmResult) {
        int nextVersion = resolveNextVersion(ctx.applicationId());
        AiTalentProfile entity = new AiTalentProfile();
        entity.setCandidateId(ctx.candidateId());
        entity.setApplicationId(ctx.applicationId());
        entity.setProfileSummary(llmResult.getProfileSummary());
        entity.setProfileTags(toJson(llmResult.getProfileTags()));
        entity.setModelId(resolveModelId());
        entity.setVersion(nextVersion);
        profileMapper.insert(entity);
        return entity;
    }

    private int resolveNextVersion(Long applicationId) {
        if (applicationId == null) {
            return 1;
        }
        AiTalentProfile latest = profileMapper.selectOne(
                new LambdaQueryWrapper<AiTalentProfile>()
                        .eq(AiTalentProfile::getApplicationId, applicationId)
                        .orderByDesc(AiTalentProfile::getVersion)
                        .last("LIMIT 1"),
                false);
        return latest == null ? 1 : latest.getVersion() + 1;
    }

    private Long resolveModelId() {
        AiModel model = modelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>().eq(AiModel::getModelCode, DEFAULT_MODEL_CODE).last("LIMIT 1"),
                false);
        return model != null ? model.getId() : null;
    }

    private TalentProfileVO toVO(AiTalentProfile entity) {
        TalentProfileVO vo = new TalentProfileVO();
        vo.setProfileId(entity.getId());
        vo.setCandidateId(entity.getCandidateId());
        vo.setApplicationId(entity.getApplicationId());
        vo.setProfileSummary(entity.getProfileSummary());
        vo.setProfileTags(parseTags(entity.getProfileTags()));
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<String> parseTags(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new AgentBusinessException("画像标签序列化失败：" + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDataMap(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        Object code = response.get("code");
        if (code instanceof Number number && number.intValue() != 200) {
            throw new AgentBusinessException(String.valueOf(response.getOrDefault("msg", "远程调用失败")));
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDataList(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }
        Object code = response.get("code");
        if (code instanceof Number number && number.intValue() != 200) {
            return List.of();
        }
        Object data = response.get("data");
        if (!(data instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private Long firstLong(Long preferred, Object fallback) {
        if (preferred != null) {
            return preferred;
        }
        return longVal(fallback);
    }

    private String firstString(String preferred, Object fallback) {
        if (StringUtils.hasText(preferred)) {
            return preferred;
        }
        return stringVal(fallback);
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record ProfileContext(
            Long candidateId,
            String candidateName,
            Long resumeId,
            Long applicationId,
            Long jobId,
            String jobTitle,
            Integer applicationStatus) {}
}
