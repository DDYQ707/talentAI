package com.talent.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.LlmMatchOutcome;
import com.talent.agent.domain.dto.ParsedMatchDto;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.LlmChatService;
import com.talent.agent.service.ResumeJobMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeJobMatchServiceImpl implements ResumeJobMatchService {

    private static final String SYSTEM_PROMPT =
            """
            你是专业的人岗匹配评估助手。请根据岗位 JD 与候选人简历结构化 JSON 进行匹配分析。
            要求：
            1. 仅输出合法 JSON，不要 Markdown 代码块，不要附加说明文字
            2. matchScore 为 0-100 的整数；advantages 约 3 条，disadvantages 约 2 条
            3. suggestedQuestions 至少 5 条，用于面试官会前准备
            4. 字段缺失时使用 null 或空数组，不要编造不存在的信息
            """;

    private static final String JSON_SCHEMA =
            """
            {
              "matchScore": 85,
              "matchLevel": "高度匹配",
              "matchReason": "综合匹配理由，100字以内",
              "advantages": ["优势1", "优势2", "优势3"],
              "disadvantages": ["不足1", "不足2"],
              "suggestedQuestions": ["建议面试问题1", "建议面试问题2", "建议面试问题3", "建议面试问题4", "建议面试问题5"],
              "dimensionScores": {
                "skill": 90,
                "experience": 80,
                "education": 75
              }
            }
            """;

    private final LlmChatService llmChatService;
    private final ObjectMapper objectMapper;

    @Override
    public LlmMatchOutcome match(JobBriefInfo job, String resumeParsedJson) {
        if (job == null) {
            throw new AgentBusinessException("岗位信息不能为空");
        }
        if (!StringUtils.hasText(resumeParsedJson)) {
            throw new AgentBusinessException("简历结构化数据不能为空");
        }

        String userPrompt = buildUserPrompt(job, resumeParsedJson.trim());
        String llmResponse = llmChatService.chat(SYSTEM_PROMPT, userPrompt);
        String jsonText = extractJson(llmResponse);

        try {
            ParsedMatchDto match = objectMapper.readValue(jsonText, ParsedMatchDto.class);
            normalizeScore(match);
            return new LlmMatchOutcome(match, jsonText);
        } catch (Exception e) {
            log.warn("LLM 匹配 JSON 解析失败，响应前 200 字符: {}", preview(llmResponse));
            throw new AgentBusinessException("人岗匹配 JSON 解析失败：" + e.getMessage(), e);
        }
    }

    private void normalizeScore(ParsedMatchDto match) {
        if (match.getMatchScore() == null) {
            return;
        }
        int score = Math.max(0, Math.min(100, match.getMatchScore()));
        match.setMatchScore(score);
    }

    private String buildUserPrompt(JobBriefInfo job, String resumeParsedJson) {
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

        return """
                请评估以下岗位与候选人的匹配度，严格遵守以下 JSON Schema（字段名不可更改）：
                """
                + JSON_SCHEMA
                + """

                岗位信息：
                """
                + jobText
                + """

                候选人简历结构化 JSON：
                """
                + resumeParsedJson;
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
