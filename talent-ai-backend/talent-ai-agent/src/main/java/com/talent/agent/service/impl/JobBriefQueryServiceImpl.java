package com.talent.agent.service.impl;

import com.talent.agent.client.JobFeignClient;
import com.talent.agent.domain.dto.JobBriefInfo;
import com.talent.agent.exception.AgentBusinessException;
import java.util.Map;
import com.talent.agent.service.JobBriefQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobBriefQueryServiceImpl implements JobBriefQueryService {

    private final JobFeignClient jobFeignClient;

    @Override
    public JobBriefInfo fetchJobBrief(Long jobId) {
        if (jobId == null) {
            throw new AgentBusinessException("jobId 不能为空");
        }
        Map<String, Object> res = jobFeignClient.getJobPostBrief(jobId);
        if (res == null || res.isEmpty()) {
            throw new AgentBusinessException("岗位不存在: " + jobId);
        }
        Object code = res.get("code");
        if (code instanceof Number codeNum && codeNum.intValue() != 200) {
            throw new AgentBusinessException(String.valueOf(res.getOrDefault("msg", "岗位查询失败")));
        }
        Object data = res.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new AgentBusinessException("岗位摘要数据为空");
        }
        JobBriefInfo info = new JobBriefInfo();
        info.setId(longVal(dataMap.get("id")));
        info.setTitle(stringVal(dataMap.get("title")));
        info.setWorkCity(stringVal(dataMap.get("workCity")));
        info.setJobDescription(stringVal(dataMap.get("jobDescription")));
        info.setJobRequirements(stringVal(dataMap.get("jobRequirements")));
        info.setSkillTags(stringVal(dataMap.get("skillTags")));
        info.setExperienceYearsMin(intVal(dataMap.get("experienceYearsMin")));
        info.setEducationRequirement(intVal(dataMap.get("educationRequirement")));
        return info;
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intVal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
