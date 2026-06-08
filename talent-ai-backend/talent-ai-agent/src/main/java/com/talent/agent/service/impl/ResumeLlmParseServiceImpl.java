package com.talent.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.LlmParseOutcome;
import com.talent.agent.domain.dto.ParsedResumeDto;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.LlmChatService;
import com.talent.agent.service.ResumeLlmParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeLlmParseServiceImpl implements ResumeLlmParseService {

    private static final int MAX_RESUME_TEXT_LENGTH = 12000;

    private static final String SYSTEM_PROMPT =
            """
            你是专业的简历结构化解析助手。请根据用户提供的简历纯文本提取信息。
            要求：
            1. 仅输出合法 JSON，不要 Markdown 代码块，不要附加任何说明文字
            2. 字段缺失时使用 null 或空数组 []，不要编造不存在的信息
            3. JSON 必须可被标准 JSON 解析器直接解析
            """;

    private static final String JSON_SCHEMA =
            """
            {
              "basicInfo": {
                "name": "姓名",
                "gender": "男/女",
                "age": 28,
                "phone": "13800138000",
                "email": "zhang@example.com",
                "city": "上海",
                "summary": "个人简介"
              },
              "education": [
                {
                  "school": "某某大学",
                  "degree": "本科",
                  "major": "计算机科学",
                  "startDate": "2015-09",
                  "endDate": "2019-06"
                }
              ],
              "workExperience": [
                {
                  "company": "某某科技",
                  "title": "Java开发工程师",
                  "startDate": "2019-07",
                  "endDate": "2024-06",
                  "description": "职责描述"
                }
              ],
              "skills": ["Java", "Spring Boot", "MySQL"],
              "projects": [
                {
                  "name": "项目名称",
                  "role": "后端负责人",
                  "description": "项目描述"
                }
              ],
              "certificates": ["软考中级"],
              "totalYears": 5.0,
              "industryKeywords": ["互联网", "招聘"],
              "targetPosition": "Java后端工程师"
            }
            """;

    private final LlmChatService llmChatService;
    private final ObjectMapper objectMapper;

    @Override
    public LlmParseOutcome parse(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            throw new AgentBusinessException("简历文本为空，无法结构化解析");
        }

        String resumeText = rawText.trim();
        if (resumeText.length() > MAX_RESUME_TEXT_LENGTH) {
            log.warn("简历文本超长，已截断至 {} 字符（原长度 {}）", MAX_RESUME_TEXT_LENGTH, resumeText.length());
            resumeText = resumeText.substring(0, MAX_RESUME_TEXT_LENGTH);
        }

        String userPrompt = buildUserPrompt(resumeText);
        String llmResponse = llmChatService.chat(SYSTEM_PROMPT, userPrompt);
        String jsonText = extractJson(llmResponse);

        try {
            ParsedResumeDto resume = objectMapper.readValue(jsonText, ParsedResumeDto.class);
            return new LlmParseOutcome(resume, jsonText);
        } catch (Exception e) {
            log.warn("LLM 返回 JSON 解析失败，响应前 200 字符: {}", preview(llmResponse));
            throw new AgentBusinessException("简历结构化 JSON 解析失败：" + e.getMessage(), e);
        }
    }

    private String buildUserPrompt(String resumeText) {
        return """
                请将以下简历文本结构化为 JSON，严格遵守以下 Schema（字段名不可更改）：
                """
                + JSON_SCHEMA
                + """

                简历文本：
                """
                + resumeText;
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
