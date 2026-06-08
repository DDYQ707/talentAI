package com.talent.agent.service;

import com.talent.agent.domain.dto.LlmParseOutcome;

/**
 * 简历解析结果持久化
 */
public interface AiResumeParseResultService {

    /**
     * 写入 ai_resume_parse_result（同一 task 仅成功时 insert 一次）
     */
    void save(Long taskId, Long resumeId, LlmParseOutcome outcome, int rawTextLength);
}
