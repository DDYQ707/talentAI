package com.talent.agent.service;

import com.talent.agent.domain.dto.LlmParseOutcome;

/**
 * 简历纯文本 → LLM 结构化 JSON
 */
public interface ResumeLlmParseService {

    /**
     * @param rawText Tika 抽取并归一化后的简历纯文本
     * @return 结构化 DTO 与原始 JSON 字符串
     */
    LlmParseOutcome parse(String rawText);
}
