package com.talent.agent.service;

import com.talent.agent.domain.dto.JobBriefInfo;

public interface JobBriefQueryService {

    JobBriefInfo fetchJobBrief(Long jobId);
}
