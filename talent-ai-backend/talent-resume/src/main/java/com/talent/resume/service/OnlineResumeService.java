package com.talent.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.resume.dto.OnlineResumeCertificateDTO;
import com.talent.resume.dto.OnlineResumeEducationDTO;
import com.talent.resume.dto.OnlineResumeProjectDTO;
import com.talent.resume.dto.OnlineResumeSaveRequest;
import com.talent.resume.dto.OnlineResumeSkillDTO;
import com.talent.resume.dto.OnlineResumeWorkDTO;
import com.talent.resume.entity.Resume;
import com.talent.resume.entity.ResumeAttachment;
import com.talent.resume.entity.ResumeCertificate;
import com.talent.resume.entity.ResumeEducation;
import com.talent.resume.entity.ResumeProject;
import com.talent.resume.entity.ResumeSkill;
import com.talent.resume.entity.ResumeWorkExperience;
import com.talent.resume.feign.AuthFeignClient;
import com.talent.resume.mapper.ResumeAttachmentMapper;
import com.talent.resume.mapper.ResumeCertificateMapper;
import com.talent.resume.mapper.ResumeEducationMapper;
import com.talent.resume.mapper.ResumeMapper;
import com.talent.resume.mapper.ResumeProjectMapper;
import com.talent.resume.mapper.ResumeSkillMapper;
import com.talent.resume.mapper.ResumeWorkExperienceMapper;
import com.talent.resume.vo.OnlineResumeDetailVO;
import com.talent.resume.vo.OnlineResumeListVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OnlineResumeService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ResumeMapper resumeMapper;
    private final ResumeAttachmentMapper attachmentMapper;
    private final ResumeEducationMapper educationMapper;
    private final ResumeWorkExperienceMapper workExperienceMapper;
    private final ResumeSkillMapper skillMapper;
    private final ResumeProjectMapper projectMapper;
    private final ResumeCertificateMapper certificateMapper;
    private final ResumeConsolidationService consolidationService;
    private final AuthFeignClient authFeignClient;

    public List<OnlineResumeListVO> listMyOnlineResumes(Long candidateId) {
        Resume primary = consolidationService.getPrimaryResume(candidateId);
        if (primary == null) {
            return List.of();
        }
        OnlineResumeListVO vo = new OnlineResumeListVO();
        vo.setId(primary.getId());
        vo.setResumeName(primary.getResumeName());
        vo.setSummary(primary.getSummary());
        vo.setIsDefault(primary.getIsDefault());
        vo.setUpdatedAt(primary.getUpdatedAt());
        vo.setCompleteness(calcCompleteness(primary.getId(), candidateId));
        return List.of(vo);
    }

    public OnlineResumeDetailVO getDetail(Long candidateId, Long resumeId) {
        Resume resume = requireOnlineResume(candidateId, resumeId);
        return buildDetailVo(resume);
    }

    @Transactional(rollbackFor = Exception.class)
    public OnlineResumeDetailVO create(Long candidateId, OnlineResumeSaveRequest request) {
        Resume resume = consolidationService.getOrCreatePrimaryResume(candidateId);
        consolidationService.clearOnlineContent(resume.getId());

        resume.setResumeName(resolveResumeName(request, "我的在线简历"));
        resume.setSummary(trimToNull(request.getSummary()));
        resume.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 1);
        resume.setScreenStatus(1);
        resumeMapper.updateById(resume);

        saveChildren(resume.getId(), request);
        consolidationService.consolidateCandidateResumes(candidateId, resume.getId());
        return buildDetailVo(resumeMapper.selectById(resume.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public OnlineResumeDetailVO update(Long candidateId, Long resumeId, OnlineResumeSaveRequest request) {
        Resume resume = requireOwnedResume(candidateId, resumeId);
        if (StringUtils.hasText(request.getResumeName())) {
            resume.setResumeName(request.getResumeName().trim());
        }
        if (request.getSummary() != null) {
            resume.setSummary(trimToNull(request.getSummary()));
        }
        if (request.getIsDefault() != null) {
            resume.setIsDefault(request.getIsDefault());
        }
        resumeMapper.updateById(resume);
        replaceChildren(resumeId, request);
        consolidationService.consolidateCandidateResumes(candidateId, resumeId);
        return buildDetailVo(resumeMapper.selectById(resumeId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long candidateId, Long resumeId) {
        requireOwnedResume(candidateId, resumeId);
        consolidationService.clearOnlineContent(resumeId);
        if (!consolidationService.hasAttachment(resumeId)) {
            resumeMapper.deleteById(resumeId);
        }
    }

    private Resume requireOwnedResume(Long candidateId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !candidateId.equals(resume.getCandidateId())) {
            throw new IllegalArgumentException("简历不存在或无权访问");
        }
        return resume;
    }

    private Resume requireOnlineResume(Long candidateId, Long resumeId) {
        return requireOwnedResume(candidateId, resumeId);
    }

    private boolean hasAttachment(Long resumeId) {
        Long count = attachmentMapper.selectCount(
                new LambdaQueryWrapper<ResumeAttachment>().eq(ResumeAttachment::getResumeId, resumeId));
        return count != null && count > 0;
    }

    private Set<Long> loadResumeIdsWithAttachment(List<Long> resumeIds) {
        if (resumeIds.isEmpty()) {
            return Set.of();
        }
        List<ResumeAttachment> list = attachmentMapper.selectList(
                new LambdaQueryWrapper<ResumeAttachment>().in(ResumeAttachment::getResumeId, resumeIds));
        return list.stream().map(ResumeAttachment::getResumeId).collect(Collectors.toCollection(HashSet::new));
    }

    private void replaceChildren(Long resumeId, OnlineResumeSaveRequest request) {
        educationMapper.delete(new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resumeId));
        workExperienceMapper.delete(
                new LambdaQueryWrapper<ResumeWorkExperience>().eq(ResumeWorkExperience::getResumeId, resumeId));
        skillMapper.delete(new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId));
        projectMapper.delete(new LambdaQueryWrapper<ResumeProject>().eq(ResumeProject::getResumeId, resumeId));
        certificateMapper.delete(
                new LambdaQueryWrapper<ResumeCertificate>().eq(ResumeCertificate::getResumeId, resumeId));
        saveChildren(resumeId, request);
    }

    private void saveChildren(Long resumeId, OnlineResumeSaveRequest request) {
        if (request == null) {
            return;
        }
        saveEducations(resumeId, request.getEducations());
        saveWorks(resumeId, request.getWorkExperiences());
        saveSkills(resumeId, request.getSkills());
        saveProjects(resumeId, request.getProjects());
        saveCertificates(resumeId, request.getCertificates());
    }

    private void saveEducations(Long resumeId, List<OnlineResumeEducationDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        int order = 0;
        for (OnlineResumeEducationDTO dto : list) {
            if (!StringUtils.hasText(dto.getSchoolName())) {
                continue;
            }
            ResumeEducation entity = new ResumeEducation();
            entity.setResumeId(resumeId);
            entity.setSchoolName(dto.getSchoolName().trim());
            entity.setMajor(trimToNull(dto.getMajor()));
            entity.setDegree(dto.getDegree());
            entity.setStartDate(parseDate(dto.getStartDate()));
            entity.setEndDate(parseDate(dto.getEndDate()));
            entity.setDescription(trimToNull(dto.getDescription()));
            entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order++);
            educationMapper.insert(entity);
        }
    }

    private void saveWorks(Long resumeId, List<OnlineResumeWorkDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        int order = 0;
        for (OnlineResumeWorkDTO dto : list) {
            if (!StringUtils.hasText(dto.getCompanyName())
                    || !StringUtils.hasText(dto.getJobTitle())
                    || !StringUtils.hasText(dto.getStartDate())) {
                continue;
            }
            ResumeWorkExperience entity = new ResumeWorkExperience();
            entity.setResumeId(resumeId);
            entity.setCompanyName(dto.getCompanyName().trim());
            entity.setJobTitle(dto.getJobTitle().trim());
            entity.setExperienceType(dto.getExperienceType() != null ? dto.getExperienceType() : 1);
            entity.setStartDate(parseDate(dto.getStartDate()));
            entity.setEndDate(parseDate(dto.getEndDate()));
            entity.setJobDescription(trimToNull(dto.getJobDescription()));
            entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order++);
            workExperienceMapper.insert(entity);
        }
    }

    private void saveSkills(Long resumeId, List<OnlineResumeSkillDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        int order = 0;
        for (OnlineResumeSkillDTO dto : list) {
            if (!StringUtils.hasText(dto.getSkillName())) {
                continue;
            }
            ResumeSkill entity = new ResumeSkill();
            entity.setResumeId(resumeId);
            entity.setSkillName(dto.getSkillName().trim());
            entity.setProficiencyLevel(dto.getProficiencyLevel());
            entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order++);
            skillMapper.insert(entity);
        }
    }

    private void saveProjects(Long resumeId, List<OnlineResumeProjectDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        int order = 0;
        for (OnlineResumeProjectDTO dto : list) {
            if (!StringUtils.hasText(dto.getProjectName()) || !StringUtils.hasText(dto.getStartDate())) {
                continue;
            }
            ResumeProject entity = new ResumeProject();
            entity.setResumeId(resumeId);
            entity.setProjectName(dto.getProjectName().trim());
            entity.setRole(trimToNull(dto.getRole()));
            entity.setTechStack(trimToNull(dto.getTechStack()));
            entity.setStartDate(parseDate(dto.getStartDate()));
            entity.setEndDate(parseDate(dto.getEndDate()));
            entity.setDescription(trimToNull(dto.getDescription()));
            entity.setLinkUrl(trimToNull(dto.getLinkUrl()));
            entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order++);
            projectMapper.insert(entity);
        }
    }

    private void saveCertificates(Long resumeId, List<OnlineResumeCertificateDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        int order = 0;
        for (OnlineResumeCertificateDTO dto : list) {
            if (!StringUtils.hasText(dto.getName()) || dto.getCertType() == null) {
                continue;
            }
            ResumeCertificate entity = new ResumeCertificate();
            entity.setResumeId(resumeId);
            entity.setCertType(dto.getCertType());
            entity.setName(dto.getName().trim());
            entity.setIssuer(trimToNull(dto.getIssuer()));
            entity.setIssueDate(parseDate(dto.getIssueDate()));
            entity.setDescription(trimToNull(dto.getDescription()));
            entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order++);
            certificateMapper.insert(entity);
        }
    }

    private OnlineResumeDetailVO buildDetailVo(Resume resume) {
        Long resumeId = resume.getId();
        OnlineResumeDetailVO vo = new OnlineResumeDetailVO();
        vo.setId(resumeId);
        vo.setResumeName(resume.getResumeName());
        vo.setSummary(resume.getSummary());
        vo.setIsDefault(resume.getIsDefault());
        vo.setCreatedAt(resume.getCreatedAt());
        vo.setUpdatedAt(resume.getUpdatedAt());
        vo.setWorkYears(resolveWorkYears(resume.getCandidateId()));
        vo.setCompleteness(calcCompleteness(resumeId, resume.getCandidateId()));

        List<ResumeEducation> educations = educationMapper.selectList(
                new LambdaQueryWrapper<ResumeEducation>()
                        .eq(ResumeEducation::getResumeId, resumeId)
                        .orderByAsc(ResumeEducation::getSortOrder));
        vo.setEducations(educations.stream().map(this::toEducationDto).toList());

        List<ResumeWorkExperience> works = workExperienceMapper.selectList(
                new LambdaQueryWrapper<ResumeWorkExperience>()
                        .eq(ResumeWorkExperience::getResumeId, resumeId)
                        .orderByAsc(ResumeWorkExperience::getSortOrder));
        vo.setWorkExperiences(works.stream().map(this::toWorkDto).toList());

        List<ResumeSkill> skills = skillMapper.selectList(
                new LambdaQueryWrapper<ResumeSkill>()
                        .eq(ResumeSkill::getResumeId, resumeId)
                        .orderByAsc(ResumeSkill::getSortOrder));
        vo.setSkills(skills.stream().map(this::toSkillDto).toList());

        List<ResumeProject> projects = projectMapper.selectList(
                new LambdaQueryWrapper<ResumeProject>()
                        .eq(ResumeProject::getResumeId, resumeId)
                        .orderByAsc(ResumeProject::getSortOrder));
        vo.setProjects(projects.stream().map(this::toProjectDto).toList());

        List<ResumeCertificate> certificates = certificateMapper.selectList(
                new LambdaQueryWrapper<ResumeCertificate>()
                        .eq(ResumeCertificate::getResumeId, resumeId)
                        .orderByAsc(ResumeCertificate::getSortOrder));
        vo.setCertificates(certificates.stream().map(this::toCertificateDto).toList());
        return vo;
    }

    private OnlineResumeEducationDTO toEducationDto(ResumeEducation e) {
        OnlineResumeEducationDTO dto = new OnlineResumeEducationDTO();
        dto.setId(e.getId());
        dto.setSchoolName(e.getSchoolName());
        dto.setMajor(e.getMajor());
        dto.setDegree(e.getDegree());
        dto.setStartDate(formatDate(e.getStartDate()));
        dto.setEndDate(formatDate(e.getEndDate()));
        dto.setDescription(e.getDescription());
        dto.setSortOrder(e.getSortOrder());
        return dto;
    }

    private OnlineResumeWorkDTO toWorkDto(ResumeWorkExperience w) {
        OnlineResumeWorkDTO dto = new OnlineResumeWorkDTO();
        dto.setId(w.getId());
        dto.setCompanyName(w.getCompanyName());
        dto.setJobTitle(w.getJobTitle());
        dto.setExperienceType(w.getExperienceType());
        dto.setStartDate(formatDate(w.getStartDate()));
        dto.setEndDate(formatDate(w.getEndDate()));
        dto.setJobDescription(w.getJobDescription());
        dto.setSortOrder(w.getSortOrder());
        return dto;
    }

    private OnlineResumeSkillDTO toSkillDto(ResumeSkill s) {
        OnlineResumeSkillDTO dto = new OnlineResumeSkillDTO();
        dto.setId(s.getId());
        dto.setSkillName(s.getSkillName());
        dto.setProficiencyLevel(s.getProficiencyLevel());
        dto.setSortOrder(s.getSortOrder());
        return dto;
    }

    private OnlineResumeProjectDTO toProjectDto(ResumeProject p) {
        OnlineResumeProjectDTO dto = new OnlineResumeProjectDTO();
        dto.setId(p.getId());
        dto.setProjectName(p.getProjectName());
        dto.setRole(p.getRole());
        dto.setTechStack(p.getTechStack());
        dto.setStartDate(formatDate(p.getStartDate()));
        dto.setEndDate(formatDate(p.getEndDate()));
        dto.setDescription(p.getDescription());
        dto.setLinkUrl(p.getLinkUrl());
        dto.setSortOrder(p.getSortOrder());
        return dto;
    }

    private OnlineResumeCertificateDTO toCertificateDto(ResumeCertificate c) {
        OnlineResumeCertificateDTO dto = new OnlineResumeCertificateDTO();
        dto.setId(c.getId());
        dto.setCertType(c.getCertType());
        dto.setName(c.getName());
        dto.setIssuer(c.getIssuer());
        dto.setIssueDate(formatDate(c.getIssueDate()));
        dto.setDescription(c.getDescription());
        dto.setSortOrder(c.getSortOrder());
        return dto;
    }

    private int calcCompleteness(Long resumeId, Long candidateId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            return 0;
        }
        int score = 0;
        String summary = resume.getSummary() != null ? resume.getSummary().trim() : "";
        if (summary.length() >= 10) {
            score += 15;
        } else if (!summary.isEmpty()) {
            score += 5;
        }

        List<ResumeEducation> educations = educationMapper.selectList(
                new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resumeId));
        if (hasValidEducation(educations)) {
            score += 20;
        } else if (!educations.isEmpty()) {
            score += 10;
        }

        List<ResumeWorkExperience> works = workExperienceMapper.selectList(
                new LambdaQueryWrapper<ResumeWorkExperience>().eq(ResumeWorkExperience::getResumeId, resumeId));
        List<ResumeProject> projects = projectMapper.selectList(
                new LambdaQueryWrapper<ResumeProject>().eq(ResumeProject::getResumeId, resumeId));
        List<ResumeCertificate> certificates = certificateMapper.selectList(
                new LambdaQueryWrapper<ResumeCertificate>().eq(ResumeCertificate::getResumeId, resumeId));
        List<ResumeSkill> skills = skillMapper.selectList(
                new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId));

        boolean freshGraduate = isFreshGraduate(resolveWorkYears(candidateId));

        if (freshGraduate) {
            if (hasValidProject(projects)) {
                score += 25;
            } else if (!projects.isEmpty()) {
                score += 12;
            }
            if (hasValidWork(works)) {
                score += 5;
            }
            boolean hasDesc = hasProjectDescription(projects) || hasWorkDescription(works);
            if (hasDesc) {
                score += 10;
            }
        } else {
            if (hasValidWork(works)) {
                score += 25;
            } else if (!works.isEmpty()) {
                score += 12;
            }
            if (hasWorkDescription(works)) {
                score += 10;
            }
            if (hasValidProject(projects)) {
                score += 5;
            }
        }

        if (hasValidSkill(skills)) {
            score += 15;
        } else if (!skills.isEmpty()) {
            score += 6;
        }

        if (hasValidCertificate(certificates)) {
            score += 5;
        }

        return Math.min(100, score);
    }

    private boolean isFreshGraduate(BigDecimal workYears) {
        return workYears == null || workYears.compareTo(BigDecimal.ONE) < 0;
    }

    private BigDecimal resolveWorkYears(Long candidateId) {
        if (candidateId == null) {
            return null;
        }
        try {
            Map<String, Object> brief = authFeignClient.getCandidateProfileBrief(candidateId);
            if (brief == null || brief.get("workYears") == null) {
                return null;
            }
            Object val = brief.get("workYears");
            if (val instanceof BigDecimal bd) {
                return bd;
            }
            if (val instanceof Number n) {
                return BigDecimal.valueOf(n.doubleValue());
            }
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasValidProject(List<ResumeProject> list) {
        return list.stream().anyMatch(p ->
                StringUtils.hasText(p.getProjectName())
                        && StringUtils.hasText(p.getRole())
                        && p.getStartDate() != null);
    }

    private boolean hasProjectDescription(List<ResumeProject> list) {
        return list.stream()
                .anyMatch(p -> p.getDescription() != null && p.getDescription().trim().length() >= 10);
    }

    private boolean hasWorkDescription(List<ResumeWorkExperience> list) {
        return list.stream()
                .anyMatch(w -> w.getJobDescription() != null && w.getJobDescription().trim().length() >= 10);
    }

    private boolean hasValidCertificate(List<ResumeCertificate> list) {
        return list.stream().anyMatch(c ->
                StringUtils.hasText(c.getName()) && c.getCertType() != null);
    }

    private boolean hasValidEducation(List<ResumeEducation> list) {
        return list.stream().anyMatch(e ->
                StringUtils.hasText(e.getSchoolName()) && e.getDegree() != null);
    }

    private boolean hasValidWork(List<ResumeWorkExperience> list) {
        return list.stream().anyMatch(w ->
                StringUtils.hasText(w.getCompanyName())
                        && StringUtils.hasText(w.getJobTitle())
                        && w.getStartDate() != null);
    }

    private boolean hasValidSkill(List<ResumeSkill> list) {
        return list.stream().anyMatch(s ->
                StringUtils.hasText(s.getSkillName()) && s.getProficiencyLevel() != null);
    }

    private String resolveResumeName(OnlineResumeSaveRequest request, String defaultName) {
        if (request != null && StringUtils.hasText(request.getResumeName())) {
            return request.getResumeName().trim();
        }
        return defaultName;
    }

    private LocalDate parseDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FMT);
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }
}
