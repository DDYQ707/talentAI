package com.talent.agent.service;

import com.talent.agent.domain.dto.LlmParseOutcome;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.dto.ParsedResumeDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ResumeParseBackfillInvoker {

    void backfillAfterParse(Long taskId, ParseTaskRequest request, LlmParseOutcome outcome);

    void markParseFailed(Long resumeId);
}
