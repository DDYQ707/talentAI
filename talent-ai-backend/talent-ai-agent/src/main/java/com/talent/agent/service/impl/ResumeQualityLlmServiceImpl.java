package com.talent.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.LlmResumeQualityOutcome;
import com.talent.agent.domain.dto.ParsedResumeQualityDto;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.LlmChatService;
import com.talent.agent.service.ResumeQualityLlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeQualityLlmServiceImpl implements ResumeQualityLlmService {

    private static final String SYSTEM_PROMPT =
            """
            你是专业的简历质量评估顾问。请根据候选人简历的结构化 JSON，评估简历本身的质量（不是与某个岗位的匹配度）。
            要求：
            1. 仅输出合法 JSON，不要 Markdown 代码块，不要附加说明文字
            2. qualityScore 为 0-100 的整数；strengths 约 3 条，weaknesses 约 2 条，suggestions 约 3 条
            3. 评价应客观，基于简历内容完整度、经历描述清晰度、量化成果、技能呈现等
            4. 不要编造简历中不存在的信息
            """;

    private static final String JSON_SCHEMA =
            """
            {
              "qualityScore": 82,
              "summary": "综合评价，80字以内",
              "strengths": ["技术栈完整", "经历详实", "项目描述清晰"],
              "weaknesses": ["量化成果不足", "缺少证书模块"],
              "suggestions": ["补充量化成果", "可增加作品集链接", "完善自我评价"],
              "dimensionScores": {
                "completeness": 85,
                "clarity": 80,
                "highlight": 75
              }
            }
            """;

    private final LlmChatService llmChatService;
    private final ObjectMapper objectMapper;

    @Override
    public LlmResumeQualityOutcome evaluate(String resumeParsedJson) {
        if (!StringUtils.hasText(resumeParsedJson)) {
            throw new AgentBusinessException("简历结构化数据不能为空");
        }

        String userPrompt =
                """
                请评估以下简历的质量（与人岗匹配无关），严格遵守 JSON Schema（字段名不可更改）：
                """
                        + JSON_SCHEMA
                        + """

                候选人简历结构化 JSON：
                """
                        + resumeParsedJson.trim();

        String llmResponse = llmChatService.chat(SYSTEM_PROMPT, userPrompt);
        String jsonText = extractJson(llmResponse);

        try {
            ParsedResumeQualityDto quality = objectMapper.readValue(jsonText, ParsedResumeQualityDto.class);
            normalizeScore(quality);
            return new LlmResumeQualityOutcome(quality, jsonText);
        } catch (Exception e) {
            log.warn("简历质量评分 JSON 解析失败，响应前 200 字符: {}", preview(llmResponse));
            throw new AgentBusinessException("简历质量评分 JSON 解析失败：" + e.getMessage(), e);
        }
    }

    private void normalizeScore(ParsedResumeQualityDto quality) {
        if (quality.getQualityScore() == null) {
            return;
        }
        quality.setQualityScore(Math.max(0, Math.min(100, quality.getQualityScore())));
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

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
