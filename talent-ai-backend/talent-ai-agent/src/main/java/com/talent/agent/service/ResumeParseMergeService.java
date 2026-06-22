package com.talent.agent.service;

import com.talent.agent.domain.dto.LlmParseOutcome;
import com.talent.agent.domain.dto.ParsedResumeDto;

/**
 * 附件 LLM 解析结果与在线简历结构化数据合并（方案 C：附件为主、在线补充）。
 */
public interface ResumeParseMergeService {

    LlmParseOutcome merge(LlmParseOutcome attachmentOutcome, ParsedResumeDto onlineSupplement);
}
