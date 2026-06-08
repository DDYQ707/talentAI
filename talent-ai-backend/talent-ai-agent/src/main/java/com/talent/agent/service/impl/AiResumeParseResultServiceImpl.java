package com.talent.agent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.LlmParseOutcome;
import com.talent.agent.domain.dto.ParsedResumeDto;
import com.talent.agent.domain.entity.AiResumeParseResult;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.mapper.AiResumeParseResultMapper;
import com.talent.agent.service.AiResumeParseResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiResumeParseResultServiceImpl implements AiResumeParseResultService {

    private final AiResumeParseResultMapper parseResultMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void save(Long taskId, Long resumeId, LlmParseOutcome outcome, int rawTextLength) {
        ParsedResumeDto resume = outcome.resume();
        AiResumeParseResult entity = new AiResumeParseResult();
        entity.setTaskId(taskId);
        entity.setResumeId(resumeId);
        entity.setParsedJson(outcome.rawJson());
        entity.setBasicInfo(toJson(resume.getBasicInfo()));
        entity.setEducationJson(toJson(resume.getEducation()));
        entity.setWorkJson(toJson(resume.getWorkExperience()));
        entity.setSkillsJson(toJson(resume.getSkills()));
        entity.setProjectJson(toJson(resume.getProjects()));
        entity.setCertificateJson(toJson(resume.getCertificates()));
        entity.setTotalYears(resume.getTotalYears());
        entity.setIndustryKeywords(toJson(resume.getIndustryKeywords()));
        entity.setTargetPosition(resume.getTargetPosition());
        entity.setRawTextLength(rawTextLength);
        parseResultMapper.insert(entity);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new AgentBusinessException("解析结果序列化失败：" + e.getMessage(), e);
        }
    }
}
