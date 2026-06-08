package com.talent.agent.domain.dto;

/**
 * LLM 解析产出：结构化 DTO + 原始 JSON 字符串
 */
public record LlmParseOutcome(ParsedResumeDto resume, String rawJson) {
}
