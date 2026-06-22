package com.talent.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.ParsedTalentProfileDto;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.LlmChatService;
import com.talent.agent.service.TalentProfileLlmService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TalentProfileLlmServiceImpl implements TalentProfileLlmService {

    private static final String SYSTEM_PROMPT =
            """
            你是专业的 HR 人才评估顾问。请根据候选人简历、岗位匹配分析与面试官评价，生成综合人才画像。
            要求：
            1. 仅输出合法 JSON，不要 Markdown 代码块，不要附加说明文字
            2. profileSummary 为 180-220 字的综合画像，客观中立、可用于 HR 决策参考
            3. profileTags 为 3-6 个短标签，如「技术扎实」「沟通良好」「经验匹配」「潜力型」
            4. 不要编造简历或评价中不存在的信息；若无面试评价，主要依据简历与匹配分析
            """;

    private static final String JSON_SCHEMA =
            """
            {
              "profileSummary": "该候选人具备...综合建议...",
              "profileTags": ["技术扎实", "项目经验丰富", "沟通良好"]
            }
            """;

    private final LlmChatService llmChatService;
    private final ObjectMapper objectMapper;

    @Override
    public ParsedTalentProfileDto generate(
            JobBriefInfo job,
            String resumeParsedJson,
            MatchResultVO matchResult,
            List<Map<String, Object>> interviewEvaluations,
            String candidateName,
            Integer applicationStatus) {
        if (job == null) {
            throw new AgentBusinessException("岗位信息不能为空");
        }
        if (!StringUtils.hasText(resumeParsedJson)) {
            throw new AgentBusinessException("简历结构化数据不能为空");
        }

        String userPrompt = buildUserPrompt(
                job, resumeParsedJson, matchResult, interviewEvaluations, candidateName, applicationStatus);
        String llmResponse = llmChatService.chat(SYSTEM_PROMPT, userPrompt);
        String jsonText = extractJson(llmResponse);

        try {
            ParsedTalentProfileDto result = objectMapper.readValue(jsonText, ParsedTalentProfileDto.class);
            validateProfile(result);
            return result;
        } catch (AgentBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("人才画像 JSON 解析失败，响应前 200 字符: {}", preview(llmResponse));
            throw new AgentBusinessException("人才画像 JSON 解析失败：" + e.getMessage(), e);
        }
    }

    private void validateProfile(ParsedTalentProfileDto result) {
        if (result == null || !StringUtils.hasText(result.getProfileSummary())) {
            throw new AgentBusinessException("大模型未返回有效人才画像");
        }
        result.setProfileSummary(result.getProfileSummary().trim());
        if (result.getProfileTags() != null) {
            result.setProfileTags(result.getProfileTags().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList());
        }
    }

    private String buildUserPrompt(
            JobBriefInfo job,
            String resumeParsedJson,
            MatchResultVO matchResult,
            List<Map<String, Object>> interviewEvaluations,
            String candidateName,
            Integer applicationStatus) {
        StringBuilder jobText = new StringBuilder();
        jobText.append("岗位名称: ").append(nullToEmpty(job.getTitle())).append('\n');
        if (StringUtils.hasText(job.getWorkCity())) {
            jobText.append("工作城市: ").append(job.getWorkCity()).append('\n');
        }
        if (StringUtils.hasText(job.getSkillTags())) {
            jobText.append("技能标签: ").append(job.getSkillTags()).append('\n');
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
            if (StringUtils.hasText(matchResult.getMatchLevel())) {
                matchText.append("匹配等级: ").append(matchResult.getMatchLevel()).append('\n');
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

        StringBuilder evalText = new StringBuilder();
        if (interviewEvaluations != null && !interviewEvaluations.isEmpty()) {
            int index = 1;
            for (Map<String, Object> evaluation : interviewEvaluations) {
                evalText.append("--- 评价 ").append(index++).append(" ---\n");
                appendLine(evalText, "面试官", evaluation.get("evaluatorName"));
                appendLine(evalText, "综合评分", evaluation.get("overallScore"));
                appendLine(evalText, "结论", evaluation.get("conclusionLabel"));
                appendLine(evalText, "维度得分", evaluation.get("dimensionScores"));
                appendLine(evalText, "评语", evaluation.get("comment"));
            }
        } else {
            evalText.append("暂无面试评价\n");
        }

        return """
                请生成综合人才画像，严格遵守以下 JSON Schema（字段名不可更改）：
                """
                + JSON_SCHEMA
                + """

                候选人姓名：
                """
                + nullToEmpty(candidateName)
                + """
                投递状态码（参考）："""
                + (applicationStatus != null ? applicationStatus : "未知")
                + """

                岗位信息：
                """
                + jobText
                + """

                人岗匹配分析：
                """
                + (matchText.isEmpty() ? "暂无\n" : matchText)
                + """

                面试官评价：
                """
                + evalText
                + """

                候选人简历结构化 JSON：
                """
                + resumeParsedJson.trim();
    }

    private void appendLine(StringBuilder builder, String label, Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return;
        }
        builder.append(label).append(": ").append(value).append('\n');
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
