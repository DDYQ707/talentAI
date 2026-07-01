package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.client.InterviewFeignClient;
import com.talent.agent.client.JobFeignClient;
import com.talent.agent.domain.dto.InterviewQuestionContext;
import com.talent.agent.domain.dto.InterviewQuestionGenerateRequest;
import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.ParsedInterviewQuestionsDto;
import com.talent.agent.domain.entity.AiInterviewQuestion;
import com.talent.agent.domain.entity.AiModel;
import com.talent.agent.domain.entity.AiResumeParseResult;
import com.talent.agent.domain.vo.InterviewQuestionGenerateResultVO;
import com.talent.agent.domain.vo.InterviewQuestionVO;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.mapper.AiInterviewQuestionMapper;
import com.talent.agent.mapper.AiModelMapper;
import com.talent.agent.mapper.AiResumeParseResultMapper;
import com.talent.agent.service.AiInterviewQuestionService;
import com.talent.agent.service.AiMatchService;
import com.talent.agent.service.InterviewQuestionLlmService;
import com.talent.agent.service.JobBriefQueryService;
import com.talent.agent.util.InterviewRoundUtil;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiInterviewQuestionServiceImpl implements AiInterviewQuestionService {

    private static final String DEFAULT_MODEL_CODE = "qwen-max";

    private final AiInterviewQuestionMapper questionMapper;
    private final AiResumeParseResultMapper parseResultMapper;
    private final AiModelMapper modelMapper;
    private final InterviewFeignClient interviewFeignClient;
    private final JobFeignClient jobFeignClient;
    private final JobBriefQueryService jobBriefQueryService;
    private final AiMatchService aiMatchService;
    private final InterviewQuestionLlmService interviewQuestionLlmService;

    @Override
    @Transactional
    public InterviewQuestionGenerateResultVO generate(InterviewQuestionGenerateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        Long interviewId = resolveInterviewId(request);
        InterviewContext ctx = loadInterviewContext(interviewId);

        AiResumeParseResult parseResult = loadParseResult(ctx.resumeId());
        JobBriefInfo job = jobBriefQueryService.fetchJobBrief(ctx.jobId());
        MatchResultVO matchResult = aiMatchService.getByApplicationId(ctx.applicationId());

        InterviewQuestionContext questionContext = InterviewQuestionContext.builder()
                .interviewId(ctx.interviewId())
                .roundNo(ctx.roundNo())
                .roundType(ctx.roundType())
                .roundTypeLabel(ctx.roundTypeLabel())
                .previousQuestionTexts(loadPreviousQuestionTexts(ctx.interviewId(), ctx.applicationId()))
                .previousRoundSummaries(loadPreviousRoundSummaries(ctx.interviewId(), ctx.applicationId(), ctx.roundNo()))
                .build();

        ParsedInterviewQuestionsDto llmResult = interviewQuestionLlmService.generate(
                job, parseResult.getParsedJson(), matchResult, ctx.candidateName(), questionContext);

        Long modelId = resolveModelId();
        replaceQuestions(interviewId, llmResult.getQuestions(), modelId);

        InterviewQuestionGenerateResultVO result = new InterviewQuestionGenerateResultVO();
        result.setInterviewId(interviewId);
        result.setApplicationId(ctx.applicationId());
        result.setJobId(ctx.jobId());
        result.setResumeId(ctx.resumeId());
        result.setCandidateName(ctx.candidateName());
        result.setJobTitle(ctx.jobTitle());
        result.setQuestions(listByInterviewId(interviewId));
        return result;
    }

    @Override
    public List<InterviewQuestionVO> listByInterviewId(Long interviewId) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 不能为空");
        }
        List<AiInterviewQuestion> records = questionMapper.selectList(
                new LambdaQueryWrapper<AiInterviewQuestion>()
                        .eq(AiInterviewQuestion::getInterviewId, interviewId)
                        .orderByAsc(AiInterviewQuestion::getSortOrder)
                        .orderByAsc(AiInterviewQuestion::getId));
        return records.stream().map(this::toVO).toList();
    }

    private Long resolveInterviewId(InterviewQuestionGenerateRequest request) {
        if (request.getInterviewId() != null) {
            return request.getInterviewId();
        }
        if (request.getApplicationId() == null) {
            throw new IllegalArgumentException("interviewId 与 applicationId 至少提供一个");
        }
        Map<String, Object> response = interviewFeignClient.listByApplication(request.getApplicationId());
        List<Map<String, Object>> records = extractDataList(response);
        if (records.isEmpty()) {
            throw new AgentBusinessException("该投递暂无面试安排，请先安排面试后再生成题目");
        }
        Map<String, Object> latest = records.get(0);
        Long interviewId = longVal(latest.get("interviewId"));
        if (interviewId == null) {
            throw new AgentBusinessException("面试记录数据异常，缺少 interviewId");
        }
        return interviewId;
    }

    private InterviewContext loadInterviewContext(Long interviewId) {
        Map<String, Object> response = interviewFeignClient.getBrief(interviewId);
        Map<String, Object> data = extractDataMap(response);
        if (data == null) {
            throw new AgentBusinessException("面试记录不存在: " + interviewId);
        }

        Long applicationId = longVal(data.get("applicationId"));
        Long jobId = longVal(data.get("jobId"));
        if (applicationId == null || jobId == null) {
            throw new AgentBusinessException("面试记录缺少 applicationId 或 jobId");
        }

        Map<String, Object> appResponse = jobFeignClient.getApplicationById(applicationId);
        Map<String, Object> appData = extractDataMap(appResponse);
        if (appData == null) {
            throw new AgentBusinessException("投递记录不存在: " + applicationId);
        }

        Long resumeId = longVal(appData.get("resumeId"));
        if (resumeId == null) {
            throw new AgentBusinessException("投递记录缺少 resumeId");
        }

        return new InterviewContext(
                interviewId,
                applicationId,
                jobId,
                resumeId,
                stringVal(data.get("candidateName")),
                stringVal(data.get("jobTitle")),
                intVal(data.get("roundNo")),
                intVal(data.get("roundType")),
                InterviewRoundUtil.roundTypeLabel(intVal(data.get("roundType"))));
    }

    private List<String> loadPreviousQuestionTexts(Long currentInterviewId, Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }
        Set<Long> otherInterviewIds = new LinkedHashSet<>();
        for (Map<String, Object> row : extractDataList(interviewFeignClient.listByApplication(applicationId))) {
            Long id = longVal(row.get("interviewId"));
            if (id != null && !id.equals(currentInterviewId)) {
                otherInterviewIds.add(id);
            }
        }
        if (otherInterviewIds.isEmpty()) {
            return List.of();
        }
        List<AiInterviewQuestion> records = questionMapper.selectList(
                new LambdaQueryWrapper<AiInterviewQuestion>()
                        .in(AiInterviewQuestion::getInterviewId, otherInterviewIds)
                        .orderByAsc(AiInterviewQuestion::getSortOrder)
                        .orderByAsc(AiInterviewQuestion::getId));
        List<String> texts = new ArrayList<>();
        for (AiInterviewQuestion record : records) {
            if (StringUtils.hasText(record.getQuestionText())) {
                texts.add(record.getQuestionText().trim());
            }
        }
        return texts;
    }

    private List<String> loadPreviousRoundSummaries(Long currentInterviewId, Long applicationId, Integer currentRoundNo) {
        if (applicationId == null) {
            return List.of();
        }
        int currentRound = currentRoundNo != null && currentRoundNo > 0 ? currentRoundNo : 1;
        List<String> summaries = new ArrayList<>();

        for (Map<String, Object> row : extractDataList(interviewFeignClient.listByApplication(applicationId))) {
            Long id = longVal(row.get("interviewId"));
            if (id != null && id.equals(currentInterviewId)) {
                continue;
            }
            Integer roundNo = intVal(row.get("roundNo"));
            Integer status = intVal(row.get("status"));
            if (roundNo == null || roundNo >= currentRound) {
                continue;
            }
            if (status != null && status == 4) {
                continue;
            }
            String typeLabel = InterviewRoundUtil.roundTypeLabel(intVal(row.get("roundType")));
            if (status != null && status == 2) {
                summaries.add("第" + roundNo + "轮(" + typeLabel + ")已完成，后续轮次应在此基础上递进加深");
            } else if (status != null && status == 1) {
                summaries.add("第" + roundNo + "轮(" + typeLabel + ")待面试，本轮勿重复其基础考察点");
            }
        }

        for (Map<String, Object> eval : extractDataList(interviewFeignClient.listEvaluationsByApplication(applicationId))) {
            Long evalInterviewId = longVal(eval.get("interviewId"));
            if (evalInterviewId != null && evalInterviewId.equals(currentInterviewId)) {
                continue;
            }
            Integer roundNo = intVal(eval.get("roundNo"));
            if (roundNo != null && roundNo >= currentRound) {
                continue;
            }
            StringBuilder line = new StringBuilder();
            line.append("第").append(roundNo != null ? roundNo : "?").append("轮");
            String typeLabel = stringVal(eval.get("roundTypeLabel"));
            if (StringUtils.hasText(typeLabel)) {
                line.append("(").append(typeLabel).append(")");
            }
            line.append("评价：");
            if (eval.get("overallScore") != null) {
                line.append("得分 ").append(eval.get("overallScore"));
            }
            String conclusion = stringVal(eval.get("conclusionLabel"));
            if (StringUtils.hasText(conclusion)) {
                line.append("，结论 ").append(conclusion);
            }
            String comment = stringVal(eval.get("comment"));
            if (StringUtils.hasText(comment)) {
                String trimmed = comment.trim();
                line.append("，评语 ").append(trimmed.length() > 80 ? trimmed.substring(0, 80) + "…" : trimmed);
            }
            summaries.add(line.toString());
        }
        return summaries;
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

    private void replaceQuestions(
            Long interviewId, List<ParsedInterviewQuestionsDto.QuestionItem> items, Long modelId) {
        questionMapper.delete(new LambdaQueryWrapper<AiInterviewQuestion>()
                .eq(AiInterviewQuestion::getInterviewId, interviewId));

        int order = 0;
        for (ParsedInterviewQuestionsDto.QuestionItem item : items) {
            AiInterviewQuestion entity = new AiInterviewQuestion();
            entity.setInterviewId(interviewId);
            entity.setQuestionText(item.getQuestionText().trim());
            entity.setCategory(StringUtils.hasText(item.getCategory()) ? item.getCategory().trim() : null);
            entity.setFocusPoint(StringUtils.hasText(item.getFocusPoint()) ? item.getFocusPoint().trim() : null);
            entity.setSortOrder(order++);
            entity.setModelId(modelId);
            questionMapper.insert(entity);
        }
    }

    private Long resolveModelId() {
        AiModel model = modelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>().eq(AiModel::getModelCode, DEFAULT_MODEL_CODE).last("LIMIT 1"),
                false);
        return model != null ? model.getId() : null;
    }

    private InterviewQuestionVO toVO(AiInterviewQuestion entity) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        vo.setId(entity.getId());
        vo.setInterviewId(entity.getInterviewId());
        vo.setQuestionText(entity.getQuestionText());
        vo.setCategory(entity.getCategory());
        vo.setFocusPoint(entity.getFocusPoint());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
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

    private record InterviewContext(
            Long interviewId,
            Long applicationId,
            Long jobId,
            Long resumeId,
            String candidateName,
            String jobTitle,
            Integer roundNo,
            Integer roundType,
            String roundTypeLabel) {}
}
