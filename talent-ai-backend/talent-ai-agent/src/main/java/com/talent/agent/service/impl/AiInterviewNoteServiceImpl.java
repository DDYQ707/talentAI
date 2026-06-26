package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.client.InterviewFeignClient;
import com.talent.agent.domain.dto.InterviewNoteSaveRequest;
import com.talent.agent.domain.dto.InterviewNoteSynthesizeRequest;
import com.talent.agent.domain.dto.ParsedInterviewEvaluationDto;
import com.talent.agent.domain.entity.AiInterviewNote;
import com.talent.agent.domain.entity.AiModel;
import com.talent.agent.domain.vo.InterviewNoteVO;
import com.talent.agent.domain.vo.InterviewQuestionVO;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.mapper.AiInterviewNoteMapper;
import com.talent.agent.mapper.AiModelMapper;
import com.talent.agent.service.AiInterviewNoteService;
import com.talent.agent.service.AiInterviewQuestionService;
import com.talent.agent.service.AiMatchService;
import com.talent.agent.service.InterviewNoteLlmService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiInterviewNoteServiceImpl implements AiInterviewNoteService {

    private static final String DEFAULT_MODEL_CODE = "qwen-max";

    private final AiInterviewNoteMapper noteMapper;
    private final AiModelMapper modelMapper;
    private final InterviewFeignClient interviewFeignClient;
    private final AiMatchService aiMatchService;
    private final AiInterviewQuestionService interviewQuestionService;
    private final InterviewNoteLlmService interviewNoteLlmService;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewNoteVO getByInterview(Long interviewId, Long interviewerId) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 不能为空");
        }
        AiInterviewNote entity = findNote(interviewId, interviewerId);
        return entity == null ? null : toVO(entity);
    }

    @Override
    @Transactional
    public InterviewNoteVO save(InterviewNoteSaveRequest request, Long interviewerId) {
        if (request == null || request.getInterviewId() == null) {
            throw new IllegalArgumentException("interviewId 不能为空");
        }
        if (interviewerId == null) {
            throw new IllegalArgumentException("未登录或用户信息缺失");
        }

        AiInterviewNote entity = findNote(request.getInterviewId(), interviewerId);
        if (entity == null) {
            entity = new AiInterviewNote();
            entity.setInterviewId(request.getInterviewId());
            entity.setInterviewerId(interviewerId);
            entity.setNoteContent(trimOrNull(request.getNoteContent()));
            noteMapper.insert(entity);
        } else {
            entity.setNoteContent(trimOrNull(request.getNoteContent()));
            noteMapper.updateById(entity);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public InterviewNoteVO synthesize(InterviewNoteSynthesizeRequest request, Long interviewerId) {
        if (request == null || request.getInterviewId() == null) {
            throw new IllegalArgumentException("interviewId 不能为空");
        }
        if (interviewerId == null) {
            throw new IllegalArgumentException("未登录或用户信息缺失");
        }

        InterviewContext ctx = loadInterviewContext(request.getInterviewId());
        String noteContent = resolveNoteContent(request, interviewerId);

        MatchResultVO matchResult = aiMatchService.getByApplicationId(ctx.applicationId());
        List<InterviewQuestionVO> questions = interviewQuestionService.listByInterviewId(request.getInterviewId());
        List<String> questionTexts = questions.stream().map(InterviewQuestionVO::getQuestionText).toList();

        ParsedInterviewEvaluationDto llmResult = interviewNoteLlmService.synthesize(
                ctx.candidateName(), ctx.jobTitle(), matchResult, questionTexts, noteContent);

        AiInterviewNote entity = findNote(request.getInterviewId(), interviewerId);
        if (entity == null) {
            entity = new AiInterviewNote();
            entity.setInterviewId(request.getInterviewId());
            entity.setInterviewerId(interviewerId);
            entity.setNoteContent(noteContent);
        } else {
            entity.setNoteContent(noteContent);
        }

        entity.setAiSummary(llmResult.getSummary().trim());
        entity.setAiSuggestedScore(llmResult.getSuggestedScore());
        entity.setAiSuggestedConclusion(llmResult.getSuggestedConclusion());
        entity.setAiDimensionScores(writeJson(llmResult.getDimensionScores()));
        entity.setAiHighlights(writeJson(llmResult.getHighlights()));
        entity.setModelId(resolveModelId());

        if (entity.getId() == null) {
            noteMapper.insert(entity);
        } else {
            noteMapper.updateById(entity);
        }
        return toVO(entity);
    }

    private String resolveNoteContent(InterviewNoteSynthesizeRequest request, Long interviewerId) {
        if (StringUtils.hasText(request.getNoteContent())) {
            return request.getNoteContent().trim();
        }
        AiInterviewNote existing = findNote(request.getInterviewId(), interviewerId);
        if (existing != null && StringUtils.hasText(existing.getNoteContent())) {
            return existing.getNoteContent().trim();
        }
        throw new AgentBusinessException("请先填写面试笔记");
    }

    private AiInterviewNote findNote(Long interviewId, Long interviewerId) {
        if (interviewerId == null) {
            return noteMapper.selectOne(
                    new LambdaQueryWrapper<AiInterviewNote>()
                            .eq(AiInterviewNote::getInterviewId, interviewId)
                            .orderByDesc(AiInterviewNote::getUpdatedAt)
                            .last("LIMIT 1"),
                    false);
        }
        return noteMapper.selectOne(
                new LambdaQueryWrapper<AiInterviewNote>()
                        .eq(AiInterviewNote::getInterviewId, interviewId)
                        .eq(AiInterviewNote::getInterviewerId, interviewerId),
                false);
    }

    private InterviewContext loadInterviewContext(Long interviewId) {
        Map<String, Object> response = interviewFeignClient.getBrief(interviewId);
        Map<String, Object> data = extractDataMap(response);
        if (data == null) {
            throw new AgentBusinessException("面试记录不存在: " + interviewId);
        }
        Long applicationId = longVal(data.get("applicationId"));
        if (applicationId == null) {
            throw new AgentBusinessException("面试记录缺少 applicationId");
        }
        return new InterviewContext(
                interviewId,
                applicationId,
                stringVal(data.get("candidateName")),
                stringVal(data.get("jobTitle")));
    }

    private Long resolveModelId() {
        AiModel model = modelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>().eq(AiModel::getModelCode, DEFAULT_MODEL_CODE).last("LIMIT 1"),
                false);
        return model != null ? model.getId() : null;
    }

    private InterviewNoteVO toVO(AiInterviewNote entity) {
        InterviewNoteVO vo = new InterviewNoteVO();
        vo.setId(entity.getId());
        vo.setInterviewId(entity.getInterviewId());
        vo.setInterviewerId(entity.getInterviewerId());
        vo.setNoteContent(entity.getNoteContent());
        vo.setAiSummary(entity.getAiSummary());
        vo.setAiSuggestedScore(entity.getAiSuggestedScore());
        vo.setAiSuggestedConclusion(entity.getAiSuggestedConclusion());
        vo.setAiSuggestedConclusionLabel(conclusionLabel(entity.getAiSuggestedConclusion()));
        vo.setAiDimensionScores(readDimensionScores(entity.getAiDimensionScores()));
        vo.setAiHighlights(readStringList(entity.getAiHighlights()));
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String conclusionLabel(Integer conclusion) {
        if (conclusion == null) {
            return null;
        }
        return switch (conclusion) {
            case 1 -> "推荐通过";
            case 2 -> "待定";
            case 3 -> "不推荐";
            default -> null;
        };
    }

    private Map<String, Integer> readDimensionScores(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> readStringList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String trimOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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

    private record InterviewContext(
            Long interviewId, Long applicationId, String candidateName, String jobTitle) {}
}
