package com.talent.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.client.ResumeFeignClient;
import com.talent.agent.domain.dto.LlmParseOutcome;
import com.talent.agent.domain.dto.ParsedResumeDto;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.OnlineResumeStructuredParseService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
public class OnlineResumeStructuredParseServiceImpl implements OnlineResumeStructuredParseService {

    private static final Map<Integer, String> DEGREE_LABEL =
            Map.of(1, "大专", 2, "本科", 3, "硕士", 4, "博士");

    private final ResumeFeignClient resumeFeignClient;
    private final ObjectMapper objectMapper;

    @Override
    public LlmParseOutcome parseFromResumeId(Long resumeId) {
        if (resumeId == null) {
            throw new AgentBusinessException("resumeId 不能为空");
        }
        Map<String, Object> response = resumeFeignClient.getAiParseContext(resumeId);
        Map<String, Object> data = extractDataMap(response);
        if (data == null) {
            throw new AgentBusinessException("在线简历数据为空");
        }
        if (!"online".equalsIgnoreCase(stringVal(data.get("resumeType")))) {
            throw new AgentBusinessException("该简历不是在线简历或已有附件，请走附件解析流程");
        }

        ParsedResumeDto parsed = mapToParsedResume(data);
        try {
            String rawJson = objectMapper.writeValueAsString(parsed);
            return new LlmParseOutcome(parsed, rawJson);
        } catch (Exception e) {
            throw new AgentBusinessException("在线简历 JSON 序列化失败：" + e.getMessage(), e);
        }
    }

    @Override
    public ParsedResumeDto parseSupplementFromResumeId(Long resumeId) {
        if (resumeId == null) {
            throw new AgentBusinessException("resumeId 不能为空");
        }
        Map<String, Object> response = resumeFeignClient.getAiParseContext(resumeId);
        Map<String, Object> data = extractDataMap(response);
        if (data == null) {
            throw new AgentBusinessException("在线简历补充数据为空");
        }
        return mapToParsedResume(data);
    }

    @SuppressWarnings("unchecked")
    private ParsedResumeDto mapToParsedResume(Map<String, Object> data) {
        ParsedResumeDto dto = new ParsedResumeDto();

        ParsedResumeDto.BasicInfo basic = new ParsedResumeDto.BasicInfo();
        basic.setName(stringVal(data.get("candidateName")));
        basic.setPhone(stringVal(data.get("phone")));
        basic.setEmail(stringVal(data.get("email")));
        basic.setCity(stringVal(data.get("city")));
        basic.setSummary(firstNonBlank(stringVal(data.get("summary")), stringVal(data.get("currentTitle"))));
        dto.setBasicInfo(basic);

        dto.setEducation(mapEducationList((List<Map<String, Object>>) data.get("educations")));
        dto.setWorkExperience(mapWorkList((List<Map<String, Object>>) data.get("workExperiences")));
        dto.setSkills(mapSkillList((List<Map<String, Object>>) data.get("skills")));
        dto.setProjects(List.of());
        dto.setCertificates(List.of());
        dto.setTotalYears(calcTotalYears(dto.getWorkExperience()));
        dto.setIndustryKeywords(extractKeywords(dto.getSkills()));
        dto.setTargetPosition(stringVal(data.get("currentTitle")));

        if (CollectionUtils.isEmpty(dto.getEducation())
                && CollectionUtils.isEmpty(dto.getWorkExperience())
                && CollectionUtils.isEmpty(dto.getSkills())
                && !StringUtils.hasText(basic.getSummary())) {
            throw new AgentBusinessException("在线简历内容为空，无法生成结构化数据");
        }
        return dto;
    }

    private List<ParsedResumeDto.EducationItem> mapEducationList(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<ParsedResumeDto.EducationItem> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }
            ParsedResumeDto.EducationItem edu = new ParsedResumeDto.EducationItem();
            edu.setSchool(stringVal(item.get("schoolName")));
            edu.setMajor(stringVal(item.get("major")));
            edu.setDegree(formatDegree(item.get("degree")));
            edu.setStartDate(normalizeMonth(stringVal(item.get("startDate"))));
            edu.setEndDate(normalizeMonth(stringVal(item.get("endDate"))));
            if (StringUtils.hasText(edu.getSchool()) || StringUtils.hasText(edu.getMajor())) {
                result.add(edu);
            }
        }
        return result;
    }

    private List<ParsedResumeDto.WorkExperienceItem> mapWorkList(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<ParsedResumeDto.WorkExperienceItem> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }
            ParsedResumeDto.WorkExperienceItem work = new ParsedResumeDto.WorkExperienceItem();
            work.setCompany(stringVal(item.get("companyName")));
            work.setTitle(stringVal(item.get("jobTitle")));
            work.setStartDate(normalizeMonth(stringVal(item.get("startDate"))));
            work.setEndDate(normalizeMonth(stringVal(item.get("endDate"))));
            work.setDescription(stringVal(item.get("jobDescription")));
            if (StringUtils.hasText(work.getCompany()) || StringUtils.hasText(work.getTitle())) {
                result.add(work);
            }
        }
        return result;
    }

    private List<String> mapSkillList(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String skill = stringVal(item.get("skillName"));
            if (StringUtils.hasText(skill)) {
                result.add(skill.trim());
            }
        }
        return result;
    }

    private BigDecimal calcTotalYears(List<ParsedResumeDto.WorkExperienceItem> works) {
        if (works == null || works.isEmpty()) {
            return null;
        }
        long totalMonths = 0;
        for (ParsedResumeDto.WorkExperienceItem work : works) {
            LocalDate start = parseDate(work.getStartDate());
            LocalDate end = parseDate(work.getEndDate());
            if (start == null) {
                continue;
            }
            if (end == null) {
                end = LocalDate.now();
            }
            if (!end.isBefore(start)) {
                totalMonths += ChronoUnit.MONTHS.between(start, end.plusDays(1));
            }
        }
        if (totalMonths <= 0) {
            return null;
        }
        return BigDecimal.valueOf(totalMonths)
                .divide(BigDecimal.valueOf(12), 1, RoundingMode.HALF_UP);
    }

    private List<String> extractKeywords(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        return skills.stream().limit(8).toList();
    }

    private String formatDegree(Object value) {
        if (value instanceof Number number) {
            return DEGREE_LABEL.getOrDefault(number.intValue(), String.valueOf(number.intValue()));
        }
        return stringVal(value);
    }

    private String normalizeMonth(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        String trimmed = date.trim();
        return trimmed.length() >= 7 ? trimmed.substring(0, 7) : trimmed;
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.length() >= 10) {
                return LocalDate.parse(trimmed.substring(0, 10));
            }
            if (trimmed.length() >= 7) {
                return LocalDate.parse(trimmed.substring(0, 7) + "-01");
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDataMap(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        Object code = response.get("code");
        if (code instanceof Number number && number.intValue() != 200) {
            throw new AgentBusinessException(String.valueOf(response.getOrDefault("msg", "简历服务调用失败")));
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String stringVal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
