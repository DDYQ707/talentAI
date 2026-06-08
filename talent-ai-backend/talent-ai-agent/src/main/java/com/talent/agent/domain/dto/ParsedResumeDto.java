package com.talent.agent.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * LLM 简历结构化解析结果（与 Prompt Schema 对齐）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedResumeDto {

    private BasicInfo basicInfo;
    private List<EducationItem> education;
    private List<WorkExperienceItem> workExperience;
    private List<String> skills;
    private List<ProjectItem> projects;
    private List<String> certificates;
    private BigDecimal totalYears;
    private List<String> industryKeywords;
    private String targetPosition;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BasicInfo {
        private String name;
        private String gender;
        private Integer age;
        private String phone;
        private String email;
        private String city;
        private String summary;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EducationItem {
        private String school;
        private String degree;
        private String major;
        private String startDate;
        private String endDate;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkExperienceItem {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectItem {
        private String name;
        private String role;
        private String description;
    }
}
