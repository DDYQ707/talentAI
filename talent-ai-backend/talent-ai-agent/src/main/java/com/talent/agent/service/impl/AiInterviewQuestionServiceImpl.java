package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.client.InterviewFeignClient;
import com.talent.agent.client.JobFeignClient;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        ParsedInterviewQuestionsDto llmResult = interviewQuestionLlmService.generate(
                job, parseResult.getParsedJson(), matchResult, ctx.candidateName());

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
                stringVal(data.get("jobTitle")));
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

    private record InterviewContext(
            Long interviewId,
            Long applicationId,
            Long jobId,
            Long resumeId,
            String candidateName,
            String jobTitle) {}
}
