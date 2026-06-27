package com.talent.resume.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** AI 解析结果回填在线简历（微服务内部） */
@Data
public class ParseResultBackfillRequest {

    private Long resumeId;

    private Long candidateId;

    private Long parseTaskId;

    /** attachment | merged | online */
    private String parseSource;

    private BasicInfo basicInfo;

    private List<EducationItem> education;

    private List<WorkExperienceItem> workExperience;

    private List<String> skills;

    private List<ProjectItem> projects;

    private List<String> certificates;

    private BigDecimal totalYears;

    private String targetPosition;

    @Data
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
    public static class EducationItem {
        private String school;
        private String degree;
        private String major;
        private String startDate;
        private String endDate;
    }

    @Data
    public static class WorkExperienceItem {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    public static class ProjectItem {
        private String name;
        private String role;
        private String description;
    }
}
