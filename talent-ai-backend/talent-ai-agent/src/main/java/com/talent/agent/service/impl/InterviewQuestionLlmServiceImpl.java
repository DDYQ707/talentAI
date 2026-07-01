package com.talent.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.InterviewQuestionContext;
import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.ParsedInterviewQuestionsDto;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.InterviewQuestionLlmService;
import com.talent.agent.service.LlmChatService;
import com.talent.agent.util.InterviewRoundUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewQuestionLlmServiceImpl implements InterviewQuestionLlmService {

    private static final int MIN_QUESTION_COUNT = 5;
    private static final int MAX_GENERATE_ATTEMPTS = 2;

    private static final String SYSTEM_PROMPT =
            """
            你是专业的招聘面试顾问。请根据岗位 JD、候选人简历、人岗匹配分析及【本场面试轮次】生成差异化面试提纲。
            要求：
            1. 仅输出合法 JSON，不要 Markdown 代码块，不要附加说明文字
            2. 必须生成至少 5 道题，建议 5~8 道；题目风格须严格按 HR 安排的「面试类型」生成，不得按轮次序号臆测类型
            3. 若有前几轮面试记录，须在前轮基础上递进、避免重复；若无前轮记录，则按本场类型独立出题
            4. category 可选：技术深度、项目经验、软技能、岗位契合、情景应对、薪资期望
            5. 「情景应对」题须描述具体情境并问「你会怎么做/如何处理」
            6. 「薪资期望」题仅 HR 面、终面/综合面使用，询问期望薪资、入职时间、福利关注等
            7. focusPoint 说明考察重点（30字以内）
            8. 不要编造简历中不存在的信息
            """;

    private static final String JSON_SCHEMA =
            """
            {
              "questions": [
                {
                  "questionText": "若上线前发现核心功能存在严重缺陷但 deadline 不变，你会如何推进？",
                  "category": "情景应对",
                  "focusPoint": "验证压力下的决策与沟通"
                },
                {
                  "questionText": "请说明你对本岗位的期望薪资范围及依据？",
                  "category": "薪资期望",
                  "focusPoint": "了解薪酬预期与谈判空间"
                },
                {
                  "questionText": "介绍你负责的核心项目及你在其中的职责？",
                  "category": "项目经验",
                  "focusPoint": "验证项目深度与个人贡献"
                },
                {
                  "questionText": "当与产品经理对需求优先级产生分歧时，你会如何处理？",
                  "category": "情景应对",
                  "focusPoint": "验证协作与问题解决"
                },
                {
                  "questionText": "该岗位所需关键技能你在实际工作中如何应用？",
                  "category": "岗位契合",
                  "focusPoint": "验证关键技能匹配度"
                }
              ]
            }
            """;

    private final LlmChatService llmChatService;
    private final ObjectMapper objectMapper;

    @Override
    public ParsedInterviewQuestionsDto generate(
            JobBriefInfo job,
            String resumeParsedJson,
            MatchResultVO matchResult,
            String candidateName,
            InterviewQuestionContext interviewContext) {
        if (job == null) {
            throw new AgentBusinessException("岗位信息不能为空");
        }
        if (!StringUtils.hasText(resumeParsedJson)) {
            throw new AgentBusinessException("简历结构化数据不能为空");
        }

        String userPrompt = buildUserPrompt(job, resumeParsedJson, matchResult, candidateName, interviewContext);
        AgentBusinessException lastError = null;
        for (int attempt = 1; attempt <= MAX_GENERATE_ATTEMPTS; attempt++) {
            String prompt = attempt == 1
                    ? userPrompt
                    : userPrompt
                            + "\n\n【重要】上次生成的 questions 数组不足 "
                            + MIN_QUESTION_COUNT
                            + " 道，请重新输出完整 JSON，questions 必须包含至少 "
                            + MIN_QUESTION_COUNT
                            + " 道不重复的有效面试题。";
            String llmResponse = null;
            try {
                llmResponse = llmChatService.chat(SYSTEM_PROMPT, prompt);
                String jsonText = extractJson(llmResponse);
                ParsedInterviewQuestionsDto result =
                        objectMapper.readValue(jsonText, ParsedInterviewQuestionsDto.class);
                validateQuestions(result);
                return result;
            } catch (AgentBusinessException e) {
                lastError = e;
                if (attempt >= MAX_GENERATE_ATTEMPTS || !isInsufficientQuestionCount(e)) {
                    throw e;
                }
                log.warn("面试题数量不足 {} 道，第 {} 次重试", MIN_QUESTION_COUNT, attempt);
            } catch (Exception e) {
                log.warn("面试题 JSON 解析失败，响应前 200 字符: {}", preview(llmResponse));
                throw new AgentBusinessException("面试题 JSON 解析失败：" + e.getMessage(), e);
            }
        }
        throw lastError != null ? lastError : new AgentBusinessException("面试题生成失败");
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
        if (valid.size() < MIN_QUESTION_COUNT) {
            throw new AgentBusinessException(
                    "大模型返回面试题不足 " + MIN_QUESTION_COUNT + " 道，实际 " + valid.size() + " 道");
        }
        result.setQuestions(valid);
    }

    private boolean isInsufficientQuestionCount(AgentBusinessException e) {
        String msg = e.getMessage();
        return msg != null && msg.contains("不足 " + MIN_QUESTION_COUNT + " 道");
    }

    private String buildUserPrompt(
            JobBriefInfo job,
            String resumeParsedJson,
            MatchResultVO matchResult,
            String candidateName,
            InterviewQuestionContext interviewContext) {
        InterviewQuestionContext ctx =
                interviewContext != null ? interviewContext : InterviewQuestionContext.builder().build();

        Integer roundType = ctx.getRoundType();
        Integer roundNo = ctx.getRoundNo();
        String roundLabel = StringUtils.hasText(ctx.getRoundTypeLabel())
                ? ctx.getRoundTypeLabel()
                : InterviewRoundUtil.roundTypeLabel(roundType);
        String roundGuidance = InterviewRoundUtil.roundQuestionGuidance(roundType, roundNo);

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

        StringBuilder previousText = new StringBuilder();
        List<String> previous = ctx.getPreviousQuestionTexts();
        if (previous != null && !previous.isEmpty()) {
            int index = 1;
            for (String question : previous) {
                if (StringUtils.hasText(question)) {
                    previousText.append(index++).append(". ").append(question.trim()).append('\n');
                }
            }
        }

        StringBuilder progressionText = new StringBuilder();
        List<String> summaries = ctx.getPreviousRoundSummaries();
        if (summaries != null && !summaries.isEmpty()) {
            for (String summary : summaries) {
                if (StringUtils.hasText(summary)) {
                    progressionText.append("- ").append(summary.trim()).append('\n');
                }
            }
        }

        int normalizedRound = InterviewRoundUtil.normalizeRoundNo(roundNo);

        return """
                请为【本场面试】生成至少 5 道（建议 5~8 道）递进式面试题，严格遵守以下 JSON Schema（字段名不可更改，questions 不得少于 5 项）：
                """
                + JSON_SCHEMA
                + """

                【本场面试信息】
                HR 安排的面试类型（本题纲必须严格按此类型生成）："""
                + roundLabel
                + """
                
                轮次序号：第 """
                + normalizedRound
                + " 轮（每份投递最多 "
                + InterviewRoundUtil.MAX_INTERVIEW_ROUNDS
                + """
                 轮，实际可 1～3 轮，以 HR 安排为准）

                【本场出题要求（按面试类型）】
                """
                + roundGuidance
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

                前几轮面试评价与结论（本轮须在此基础上递进，勿重复考察相同层次）：
                """
                + (progressionText.isEmpty() ? "暂无（本场无前轮记录，按 HR 安排的面试类型独立出题）\n" : progressionText)
                + """

                前几轮已使用过的题目（请勿重复或仅做微调，须生成全新角度）：
                """
                + (previousText.isEmpty() ? "暂无（本场为首轮面试）\n" : previousText)
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
