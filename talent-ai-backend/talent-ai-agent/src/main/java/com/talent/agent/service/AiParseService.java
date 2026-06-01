package com.talent.agent.service;

import com.talent.agent.domain.dto.MatchRequest;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.domain.vo.ParseTaskVO;

public interface AiParseService {

    ParseTaskVO submitParseTask(ParseTaskRequest request);

    ParseTaskVO getParseTask(Long taskId);

    ParseTaskVO getLatestByResumeId(Long resumeId);
}
