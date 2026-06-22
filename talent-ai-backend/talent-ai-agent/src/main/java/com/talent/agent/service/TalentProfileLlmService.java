package com.talent.agent.service;

import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.domain.dto.ParsedTalentProfileDto;
import com.talent.agent.domain.vo.MatchResultVO;
import java.util.List;
import java.util.Map;

public interface TalentProfileLlmService {

    ParsedTalentProfileDto generate(
            JobBriefInfo job,
            String resumeParsedJson,
            MatchResultVO matchResult,
            List<Map<String, Object>> interviewEvaluations,
            String candidateName,
            Integer applicationStatus);
}
