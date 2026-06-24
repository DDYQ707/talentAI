package com.talent.agent.service;

import com.talent.agent.domain.dto.ResumeQualityEvaluateRequest;
import com.talent.agent.domain.vo.ResumeQualityScoreVO;

public interface AiResumeQualityService {

    ResumeQualityScoreVO getLatestByResumeId(Long resumeId);

    ResumeQualityScoreVO evaluate(Long operatorUserId, ResumeQualityEvaluateRequest request);

    /** 解析成功后异步触发质量评分 */
    void evaluateAfterParseAsync(Long resumeId, Long candidateId, Long parseTaskId);
}
