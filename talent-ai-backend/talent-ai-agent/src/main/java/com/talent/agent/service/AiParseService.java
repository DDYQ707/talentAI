package com.talent.agent.service;

import com.talent.agent.domain.dto.AiParseRetryRequest;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.vo.ParseTaskVO;

public interface AiParseService {

    ParseTaskVO submitParseTask(ParseTaskRequest request);

    /** HR 手动重新解析（覆盖最新结果） */
    ParseTaskVO submitReparse(AiParseRetryRequest request);

    ParseTaskVO getParseTask(Long taskId);

    ParseTaskVO getLatestByResumeId(Long resumeId);
}
