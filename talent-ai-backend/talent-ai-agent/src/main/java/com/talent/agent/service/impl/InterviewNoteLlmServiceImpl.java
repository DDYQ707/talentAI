package com.talent.agent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.ParsedInterviewEvaluationDto;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.InterviewNoteLlmService;
import com.talent.agent.service.LlmChatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewNoteLlmServiceImpl implements InterviewNoteLlmService {

    private static final String SYSTEM_PROMPT =
            """
            你是专业的招聘面试评估顾问。请根据面试官的现场笔记、候选人信息和岗位背景，生成结构化的面试评估草稿。
            要求：
            1. 仅输出合法 JSON，不要 Markdown 代码块，不要附加说明文字
            2. summary 为 150-300 字的评估摘要，客观、具体，基于笔记内容
            3. suggestedScore 为 0-100 整数
            4. suggestedConclusion：1=推荐通过，2=待定，3=不推荐
            5. dimensionScores 包含：专业技能、沟通表达、逻辑思维、团队合作、学习能力（每项 0-100）
            6. highlights 为 2-5 条关键信号（正面或需关注），每条 15 字以内
            7. 不要编造笔记中未提及的信息
            """;

    private static final String JSON_SCHEMA =
            """
            {
              "summary": "候选人在项目经验方面表现突出...",
              "suggestedScore": 82,
              "suggestedConclusion": 1,
              "dimensionScores": {
                "专业技能": 85,
                "沟通表达": 78,
                "逻辑思维": 80,
                "团队合作": 75,
                "学习能力": 88
              },
              "highlights": ["量化成果意识强", "高并发经验需进一步验证"]
            }
            """;

    private final LlmChatService llmChatService;
    private final ObjectMapper objectMapper;

    @Override
    public ParsedInterviewEvaluationDto synthesize(
            String candidateName,
            String jobTitle,
            MatchResultVO matchResult,
            List<String> interviewQuestions,
            String noteContent) {
        if (!StringUtils.hasText(noteContent) || noteContent.trim().length() < 10) {
            throw new AgentBusinessException("面试笔记内容过少，请至少记录 10 字以上的观察");
        }

        String userPrompt = buildUserPrompt(candidateName, jobTitle, matchResult, interviewQuestions, noteContent);
        String llmResponse = llmChatService.chat(SYSTEM_PROMPT, userPrompt);
        String jsonText = extractJson(llmResponse);

        try {
            ParsedInterviewEvaluationDto result =
                    objectMapper.readValue(jsonText, ParsedInterviewEvaluationDto.class);
            validateResult(result);
            return result;
        } catch (AgentBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("面试评估草稿 JSON 解析失败，响应前 200 字符: {}", preview(llmResponse));
            throw new AgentBusinessException("评估草稿 JSON 解析失败：" + e.getMessage(), e);
        }
    }

    private void validateResult(ParsedInterviewEvaluationDto result) {
        if (result == null || !StringUtils.hasText(result.getSummary())) {
            throw new AgentBusinessException("大模型未返回有效评估摘要");
        }
        if (result.getSuggestedScore() == null) {
            result.setSuggestedScore(70);
        }
        result.setSuggestedScore(Math.min(100, Math.max(0, result.getSuggestedScore())));
        if (result.getSuggestedConclusion() == null
                || result.getSuggestedConclusion() < 1
                || result.getSuggestedConclusion() > 3) {
            result.setSuggestedConclusion(2);
        }
    }

    private String buildUserPrompt(
            String candidateName,
            String jobTitle,
            MatchResultVO matchResult,
            List<String> interviewQuestions,
            String noteContent) {
        StringBuilder matchText = new StringBuilder();
        if (matchResult != null) {
            if (matchResult.getMatchScore() != null) {
                matchText.append("AI初筛匹配分: ").append(matchResult.getMatchScore()).append('\n');
            }
            if (StringUtils.hasText(matchResult.getAdvantages())) {
                matchText.append("初筛优势: ").append(matchResult.getAdvantages()).append('\n');
            }
            if (StringUtils.hasText(matchResult.getDisadvantages())) {
                matchText.append("初筛不足: ").append(matchResult.getDisadvantages()).append('\n');
            }
        }

        StringBuilder questionsText = new StringBuilder();
        if (interviewQuestions != null && !interviewQuestions.isEmpty()) {
            for (int i = 0; i < interviewQuestions.size(); i++) {
                questionsText.append(i + 1).append(". ").append(interviewQuestions.get(i)).append('\n');
            }
        }

        return """
                请根据以下信息生成面试评估草稿，严格遵守以下 JSON Schema（字段名不可更改）：
                """
                + JSON_SCHEMA
                + """

                候选人：
                """
                + nullToEmpty(candidateName)
                + """

                应聘岗位：
                """
                + nullToEmpty(jobTitle)
                + """

                初筛参考（如有）：
                """
                + (matchText.isEmpty() ? "暂无\n" : matchText)
                + """

                已用面试题（如有）：
                """
                + (questionsText.isEmpty() ? "暂无\n" : questionsText)
                + """

                面试官现场笔记：
                """
                + noteContent.trim();
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
