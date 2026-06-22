package com.talent.agent.service;

import com.talent.agent.domain.dto.LlmParseOutcome;
import com.talent.agent.domain.dto.ParsedResumeDto;

/**
 * 将在线简历结构化数据转换为与附件解析一致的 ParsedResumeDto。
 */
public interface OnlineResumeStructuredParseService {

    /** 纯在线简历：DB 结构化映射 */
    LlmParseOutcome parseFromResumeId(Long resumeId);

    /** 合并场景：仅提取在线简历补充字段（可与附件并存） */
    ParsedResumeDto parseSupplementFromResumeId(Long resumeId);
}
