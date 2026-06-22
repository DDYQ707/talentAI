package com.talent.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.ParsedInterviewQuestionsDto;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.InterviewQuestionLlmService;
import com.talent.agent.service.LlmChatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewQuestionLlmServiceImpl implements InterviewQuestionLlmService {

    private static final String SYSTEM_PROMPT =
            """
            你是专业的招聘面试顾问。请根据岗位 JD、候选人简历结构化数据及人岗匹配分析，生成针对性面试追问。
            要求：
            1. 仅输出合法 JSON，不要 Markdown 代码块，不要附加说明文字
            2. 生成 3-5 道题，每题需针对候选人不足或需验证的能力点
            3. category 可选：技术深度、项目经验、软技能、岗位契合
            4. focusPoint 说明该题考察重点（30字以内）
            5. 不要编造简历中不存在的信息
            """;

    private static final String JSON_SCHEMA =
            """
            {
              "questions": [
                {
                  "questionText": "请描述你在高并发场景下如何设计缓存与限流策略？",
                  "category": "技术深度",
                  "focusPoint": "验证高并发与系统稳定性经验"
                }
              ]
            }
            """;

    private final LlmChatService llmChatService;
    private final ObjectMapper objectMapper;

    @Override
    public ParsedInterviewQuestionsDto generate(
            JobBriefInfo job, String resumeParsedJson, MatchResultVO matchResult, String candidateName) {
        if (job == null) {
            throw new AgentBusinessException("岗位信息不能为空");
        }
        if (!StringUtils.hasText(resumeParsedJson)) {
            throw new AgentBusinessException("简历结构化数据不能为空");
        }

        String userPrompt = buildUserPrompt(job, resumeParsedJson, matchResult, candidateName);
        String llmResponse = llmChatService.chat(SYSTEM_PROMPT, userPrompt);
        String jsonText = extractJson(llmResponse);

        try {
            ParsedInterviewQuestionsDto result = objectMapper.readValue(jsonText, ParsedInterviewQuestionsDto.class);
            validateQuestions(result);
            return result;
        } catch (AgentBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("面试题 JSON 解析失败，响应前 200 字符: {}", preview(llmResponse));
            throw new AgentBusinessException("面试题 JSON 解析失败：" + e.getMessage(), e);
        }
    }

    private void validateQuestions(ParsedInterviewQuestionsDto result) {
        if (result == null || result.getQuestions() == null || result.getQuestions().isEmpty()) {
            throw new AgentBusinessException("大模型未返回有效面试题");
        }
        List<ParsedInterviewQuestionsDto.QuestionItem> valid = result.getQuestions().stream()
                .filter(q -> q != null && StringUtils.hasText(q.getQuestionText()))
                .toList();
        if (valid.isEmpty()) {
            throw new AgentBusinessException("大模型返回的面试题内容为空");
        }
        result.setQuestions(valid);
    }

    private String buildUserPrompt(
            JobBriefInfo job, String resumeParsedJson, MatchResultVO matchResult, String candidateName) {
        StringBuilder jobText = new StringBuilder();
        jobText.append("岗位名称: ").append(nullToEmpty(job.getTitle())).append('\n');
        if (StringUtils.hasText(job.getWorkCity())) {
            jobText.append("工作城市: ").append(job.getWorkCity()).append('\n');
        }
        if (StringUtils.hasText(job.getSkillTags())) {
            jobText.append("技能标签: ").append(job.getSkillTags()).append('\n');
        }
        if (job.getExperienceYearsMin() != null) {
            jobText.append("最低工作年限: ").append(job.getExperienceYearsMin()).append('\n');
        }
        if (StringUtils.hasText(job.getJobDescription())) {
            jobText.append("职位描述:\n").append(job.getJobDescription()).append('\n');
        }
        if (StringUtils.hasText(job.getJobRequirements())) {
            jobText.append("任职要求:\n").append(job.getJobRequirements()).append('\n');
        }

        StringBuilder matchText = new StringBuilder();
        if (matchResult != null) {
            if (matchResult.getMatchScore() != null) {
                matchText.append("匹配分: ").append(matchResult.getMatchScore()).append('\n');
            }
            if (StringUtils.hasText(matchResult.getAdvantages())) {
                matchText.append("优势: ").append(matchResult.getAdvantages()).append('\n');
            }
            if (StringUtils.hasText(matchResult.getDisadvantages())) {
                matchText.append("不足: ").append(matchResult.getDisadvantages()).append('\n');
            }
            if (StringUtils.hasText(matchResult.getMatchReason())) {
                matchText.append("匹配理由: ").append(matchResult.getMatchReason()).append('\n');
            }
        }

        return """
                请为以下候选人生成 3-5 道针对性面试追问，严格遵守以下 JSON Schema（字段名不可更改）：
                """
                + JSON_SCHEMA
                + """

                候选人姓名：
                """
                + nullToEmpty(candidateName)
                + """

                岗位信息：
                """
                + jobText
                + """

                人岗匹配分析（如有）：
                """
                + (matchText.isEmpty() ? "暂无\n" : matchText)
                + """

                候选人简历结构化 JSON：
                """
                + resumeParsedJson.trim();
    }

    private String extractJson(String content) {
        if (!StringUtils.hasText(content)) {
            throw new AgentBusinessException("大模型返回内容为空");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int fence = trimmed.lastIndexOf("```");
            if (fence >= 0) {
                trimmed = trimmed.substring(0, fence);
            }
            trimmed = trimmed.trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AgentBusinessException("大模型返回内容中未找到 JSON 对象");
        }
        return trimmed.substring(start, end + 1);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
