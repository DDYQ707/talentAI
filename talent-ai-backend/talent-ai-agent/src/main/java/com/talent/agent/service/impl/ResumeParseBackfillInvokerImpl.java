package com.talent.agent.service.impl;

import com.talent.agent.client.ResumeFeignClient;
import com.talent.agent.domain.dto.LlmParseOutcome;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.dto.ParsedResumeDto;
import com.talent.agent.service.ResumeParseBackfillInvoker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeParseBackfillInvokerImpl implements ResumeParseBackfillInvoker {

    private final ResumeFeignClient resumeFeignClient;

    @Override
    public void backfillAfterParse(Long taskId, ParseTaskRequest request, LlmParseOutcome outcome) {
        if (request == null || request.getResumeId() == null || outcome == null || outcome.resume() == null) {
            return;
        }
        Map<String, Object> body = toBackfillBody(taskId, request, outcome.resume());
        try {
            Map<String, Object> res = resumeFeignClient.backfillParseResult(body);
            Object code = res != null ? res.get("code") : null;
            if (code instanceof Number n && n.intValue() == 200) {
                log.info(
                        "解析结果已触发回填 taskId={} resumeId={} parseSource={}",
                        taskId,
                        request.getResumeId(),
                        request.getParseSource());
                return;
            }
            log.warn(
                    "解析结果回填失败 taskId={} resumeId={} msg={}",
                    taskId,
                    request.getResumeId(),
                    res != null ? res.get("msg") : "null");
        } catch (Exception e) {
            log.warn(
                    "解析结果回填异常 taskId={} resumeId={} reason={}",
                    taskId,
                    request.getResumeId(),
                    e.getMessage());
        }
    }

    @Override
    public void markParseFailed(Long resumeId) {
        if (resumeId == null) {
            return;
        }
        try {
            resumeFeignClient.markParseFailed(resumeId);
        } catch (Exception e) {
            log.warn("解析失败状态回写异常 resumeId={} reason={}", resumeId, e.getMessage());
        }
    }

    private Map<String, Object> toBackfillBody(Long taskId, ParseTaskRequest request, ParsedResumeDto parsed) {
        Map<String, Object> body = new HashMap<>();
        body.put("resumeId", request.getResumeId());
        body.put("candidateId", request.getCandidateId());
        body.put("parseTaskId", taskId);
        body.put("parseSource", request.getParseSource());
        body.put("totalYears", parsed.getTotalYears());
        body.put("targetPosition", parsed.getTargetPosition());
        body.put("skills", parsed.getSkills());
        body.put("certificates", parsed.getCertificates());

        if (parsed.getBasicInfo() != null) {
            ParsedResumeDto.BasicInfo basic = parsed.getBasicInfo();
            Map<String, Object> basicMap = new HashMap<>();
            basicMap.put("name", basic.getName());
            basicMap.put("gender", basic.getGender());
            basicMap.put("age", basic.getAge());
            basicMap.put("phone", basic.getPhone());
            basicMap.put("email", basic.getEmail());
            basicMap.put("city", basic.getCity());
            basicMap.put("summary", basic.getSummary());
            body.put("basicInfo", basicMap);
        }

        body.put("education", mapEducation(parsed.getEducation()));
        body.put("workExperience", mapWork(parsed.getWorkExperience()));
        body.put("projects", mapProjects(parsed.getProjects()));
        return body;
    }

    private List<Map<String, Object>> mapEducation(List<ParsedResumeDto.EducationItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getSchool()))
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("school", item.getSchool());
                    map.put("degree", item.getDegree());
                    map.put("major", item.getMajor());
                    map.put("startDate", item.getStartDate());
                    map.put("endDate", item.getEndDate());
                    return map;
                })
                .toList();
    }

    private List<Map<String, Object>> mapWork(List<ParsedResumeDto.WorkExperienceItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null
                        && StringUtils.hasText(item.getCompany())
                        && StringUtils.hasText(item.getTitle()))
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("company", item.getCompany());
                    map.put("title", item.getTitle());
                    map.put("startDate", item.getStartDate());
                    map.put("endDate", item.getEndDate());
                    map.put("description", item.getDescription());
                    return map;
                })
                .toList();
    }

    private List<Map<String, Object>> mapProjects(List<ParsedResumeDto.ProjectItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getName()))
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", item.getName());
                    map.put("role", item.getRole());
                    map.put("description", item.getDescription());
                    return map;
                })
                .toList();
    }
}
