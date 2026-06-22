package com.talent.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.agent.domain.dto.LlmParseOutcome;
import com.talent.agent.domain.dto.ParsedResumeDto;
import com.talent.agent.exception.AgentBusinessException;
import com.talent.agent.service.ResumeParseMergeService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ResumeParseMergeServiceImpl implements ResumeParseMergeService {

    private final ObjectMapper objectMapper;

    @Override
    public LlmParseOutcome merge(LlmParseOutcome attachmentOutcome, ParsedResumeDto onlineSupplement) {
        if (attachmentOutcome == null || attachmentOutcome.resume() == null) {
            throw new AgentBusinessException("附件解析结果不能为空");
        }
        if (onlineSupplement == null) {
            return attachmentOutcome;
        }

        ParsedResumeDto merged = mergeDto(attachmentOutcome.resume(), onlineSupplement);
        try {
            String rawJson = objectMapper.writeValueAsString(merged);
            return new LlmParseOutcome(merged, rawJson);
        } catch (Exception e) {
            throw new AgentBusinessException("合并简历 JSON 序列化失败：" + e.getMessage(), e);
        }
    }

    private ParsedResumeDto mergeDto(ParsedResumeDto attachment, ParsedResumeDto online) {
        ParsedResumeDto merged = new ParsedResumeDto();
        merged.setBasicInfo(mergeBasicInfo(attachment.getBasicInfo(), online.getBasicInfo()));
        merged.setEducation(mergeEducation(attachment.getEducation(), online.getEducation()));
        merged.setWorkExperience(mergeWork(attachment.getWorkExperience(), online.getWorkExperience()));
        merged.setSkills(mergeSkills(attachment.getSkills(), online.getSkills()));
        merged.setProjects(firstNonEmpty(attachment.getProjects(), online.getProjects()));
        merged.setCertificates(firstNonEmpty(attachment.getCertificates(), online.getCertificates()));
        merged.setTargetPosition(firstNonBlank(attachment.getTargetPosition(), online.getTargetPosition()));
        merged.setIndustryKeywords(mergeKeywords(attachment.getIndustryKeywords(), online.getIndustryKeywords()));
        merged.setTotalYears(maxYears(attachment.getTotalYears(), online.getTotalYears(), merged.getWorkExperience()));
        return merged;
    }

    private ParsedResumeDto.BasicInfo mergeBasicInfo(
            ParsedResumeDto.BasicInfo attachment, ParsedResumeDto.BasicInfo online) {
        if (attachment == null && online == null) {
            return null;
        }
        ParsedResumeDto.BasicInfo merged = new ParsedResumeDto.BasicInfo();
        if (attachment != null && online != null) {
            merged.setName(firstNonBlank(attachment.getName(), online.getName()));
            merged.setGender(firstNonBlank(attachment.getGender(), online.getGender()));
            merged.setAge(attachment.getAge() != null ? attachment.getAge() : online.getAge());
            merged.setPhone(firstNonBlank(attachment.getPhone(), online.getPhone()));
            merged.setEmail(firstNonBlank(attachment.getEmail(), online.getEmail()));
            merged.setCity(firstNonBlank(attachment.getCity(), online.getCity()));
            merged.setSummary(firstNonBlank(attachment.getSummary(), online.getSummary()));
            return merged;
        }
        return attachment != null ? attachment : online;
    }

    private List<ParsedResumeDto.EducationItem> mergeEducation(
            List<ParsedResumeDto.EducationItem> attachment, List<ParsedResumeDto.EducationItem> online) {
        List<ParsedResumeDto.EducationItem> result = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        appendEducation(result, keys, attachment);
        appendEducation(result, keys, online);
        return result;
    }

    private void appendEducation(
            List<ParsedResumeDto.EducationItem> target,
            Set<String> keys,
            List<ParsedResumeDto.EducationItem> source) {
        if (source == null) {
            return;
        }
        for (ParsedResumeDto.EducationItem item : source) {
            if (item == null) {
                continue;
            }
            String key = normalize(item.getSchool()) + "|" + normalize(item.getMajor());
            if (!StringUtils.hasText(key.replace("|", "")) || keys.add(key)) {
                target.add(item);
            }
        }
    }

    private List<ParsedResumeDto.WorkExperienceItem> mergeWork(
            List<ParsedResumeDto.WorkExperienceItem> attachment, List<ParsedResumeDto.WorkExperienceItem> online) {
        List<ParsedResumeDto.WorkExperienceItem> result = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        appendWork(result, keys, attachment);
        appendWork(result, keys, online);
        return result;
    }

    private void appendWork(
            List<ParsedResumeDto.WorkExperienceItem> target,
            Set<String> keys,
            List<ParsedResumeDto.WorkExperienceItem> source) {
        if (source == null) {
            return;
        }
        for (ParsedResumeDto.WorkExperienceItem item : source) {
            if (item == null) {
                continue;
            }
            String key = normalize(item.getCompany()) + "|" + normalize(item.getTitle());
            if (!StringUtils.hasText(key.replace("|", "")) || keys.add(key)) {
                target.add(item);
            }
        }
    }

    private List<String> mergeSkills(List<String> attachment, List<String> online) {
        Set<String> merged = new LinkedHashSet<>();
        addSkills(merged, attachment);
        addSkills(merged, online);
        return new ArrayList<>(merged);
    }

    private void addSkills(Set<String> target, List<String> source) {
        if (source == null) {
            return;
        }
        for (String skill : source) {
            if (StringUtils.hasText(skill)) {
                target.add(skill.trim());
            }
        }
    }

    private List<String> mergeKeywords(List<String> attachment, List<String> online) {
        Set<String> merged = new LinkedHashSet<>();
        addSkills(merged, attachment);
        addSkills(merged, online);
        return new ArrayList<>(merged);
    }

    private <T> List<T> firstNonEmpty(List<T> first, List<T> second) {
        if (!CollectionUtils.isEmpty(first)) {
            return first;
        }
        return second == null ? List.of() : second;
    }

    private BigDecimal maxYears(
            BigDecimal attachmentYears, BigDecimal onlineYears, List<ParsedResumeDto.WorkExperienceItem> mergedWork) {
        BigDecimal fromWork = calcTotalYears(mergedWork);
        BigDecimal max = attachmentYears;
        max = max(max, onlineYears);
        max = max(max, fromWork);
        return max;
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.compareTo(right) >= 0 ? left : right;
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : (StringUtils.hasText(second) ? second.trim() : null);
    }
}
