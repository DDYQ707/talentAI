package com.talent.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.resume.dto.ParseResultBackfillRequest;
import com.talent.resume.dto.ParseProfilePatchRequest;
import com.talent.resume.entity.Resume;
import com.talent.resume.entity.ResumeCertificate;
import com.talent.resume.entity.ResumeEducation;
import com.talent.resume.entity.ResumeProject;
import com.talent.resume.entity.ResumeSkill;
import com.talent.resume.entity.ResumeWorkExperience;
import com.talent.resume.feign.AuthFeignClient;
import com.talent.resume.mapper.ResumeCertificateMapper;
import com.talent.resume.mapper.ResumeEducationMapper;
import com.talent.resume.mapper.ResumeMapper;
import com.talent.resume.mapper.ResumeProjectMapper;
import com.talent.resume.mapper.ResumeSkillMapper;
import com.talent.resume.mapper.ResumeWorkExperienceMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeParseBackfillService {

    private static final int PARSE_STATUS_SUCCESS = 2;

    private static final int PARSE_STATUS_PROCESSING = 1;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ResumeMapper resumeMapper;
    private final ResumeEducationMapper educationMapper;
    private final ResumeWorkExperienceMapper workExperienceMapper;
    private final ResumeSkillMapper skillMapper;
    private final ResumeProjectMapper projectMapper;
    private final ResumeCertificateMapper certificateMapper;
    private final ResumeConsolidationService consolidationService;
    private final AuthFeignClient authFeignClient;

    @Transactional(rollbackFor = Exception.class)
    public void backfill(ParseResultBackfillRequest request) {
        if (request == null || request.getResumeId() == null) {
            throw new IllegalArgumentException("resumeId 不能为空");
        }
        if ("online".equalsIgnoreCase(trimToNull(request.getParseSource()))) {
            log.info("在线简历解析跳过回填 resumeId={}", request.getResumeId());
            markParseSuccess(request.getResumeId());
            return;
        }

        Resume resume = resumeMapper.selectById(request.getResumeId());
        if (resume == null) {
            throw new IllegalArgumentException("简历不存在");
        }

        Resume primary = consolidationService.getPrimaryResume(resume.getCandidateId());
        Long targetResumeId = primary != null ? primary.getId() : resume.getId();
        Resume target = primary != null ? primary : resume;

        backfillSummary(target, request);
        backfillEducations(targetResumeId, request.getEducation());
        backfillWorks(targetResumeId, request.getWorkExperience());
        backfillSkills(targetResumeId, request.getSkills());
        backfillProjects(targetResumeId, request.getProjects());
        backfillCertificates(targetResumeId, request.getCertificates());
        patchCandidateProfile(resume.getCandidateId(), request);

        target.setParseStatus(PARSE_STATUS_SUCCESS);
        target.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(target);

        log.info(
                "AI 解析结果已回填 resumeId={} targetResumeId={} taskId={} parseSource={}",
                request.getResumeId(),
                targetResumeId,
                request.getParseTaskId(),
                request.getParseSource());
    }

    @Transactional(rollbackFor = Exception.class)
    public void markParseProcessing(Long resumeId) {
        if (resumeId == null) {
            return;
        }
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            return;
        }
        Resume primary = consolidationService.getPrimaryResume(resume.getCandidateId());
        Resume target = primary != null ? primary : resume;
        target.setParseStatus(PARSE_STATUS_PROCESSING);
        target.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(target);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markParseFailed(Long resumeId) {
        if (resumeId == null) {
            return;
        }
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            return;
        }
        Resume primary = consolidationService.getPrimaryResume(resume.getCandidateId());
        Resume target = primary != null ? primary : resume;
        target.setParseStatus(3);
        target.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(target);
    }

    private void markParseSuccess(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            return;
        }
        Resume primary = consolidationService.getPrimaryResume(resume.getCandidateId());
        Resume target = primary != null ? primary : resume;
        target.setParseStatus(PARSE_STATUS_SUCCESS);
        target.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(target);
    }

    private void backfillSummary(Resume resume, ParseResultBackfillRequest request) {
        String summary = null;
        if (request.getBasicInfo() != null && StringUtils.hasText(request.getBasicInfo().getSummary())) {
            summary = request.getBasicInfo().getSummary().trim();
        } else if (StringUtils.hasText(request.getTargetPosition())) {
            summary = request.getTargetPosition().trim();
        }
        if (StringUtils.hasText(summary)) {
            resume.setSummary(summary);
        }
    }

    private void backfillEducations(Long resumeId, List<ParseResultBackfillRequest.EducationItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        educationMapper.delete(new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resumeId));
        int order = 0;
        for (ParseResultBackfillRequest.EducationItem item : items) {
            if (item == null || !StringUtils.hasText(item.getSchool())) {
                continue;
            }
            ResumeEducation entity = new ResumeEducation();
            entity.setResumeId(resumeId);
            entity.setSchoolName(item.getSchool().trim());
            entity.setMajor(trimToNull(item.getMajor()));
            entity.setDegree(parseDegree(item.getDegree()));
            entity.setStartDate(parseDate(item.getStartDate()));
            entity.setEndDate(parseDate(item.getEndDate()));
            entity.setSortOrder(order++);
            educationMapper.insert(entity);
        }
    }

    private void backfillWorks(Long resumeId, List<ParseResultBackfillRequest.WorkExperienceItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        workExperienceMapper.delete(
                new LambdaQueryWrapper<ResumeWorkExperience>().eq(ResumeWorkExperience::getResumeId, resumeId));
        int order = 0;
        for (ParseResultBackfillRequest.WorkExperienceItem item : items) {
            if (item == null || !StringUtils.hasText(item.getCompany()) || !StringUtils.hasText(item.getTitle())) {
                continue;
            }
            ResumeWorkExperience entity = new ResumeWorkExperience();
            entity.setResumeId(resumeId);
            entity.setCompanyName(item.getCompany().trim());
            entity.setJobTitle(item.getTitle().trim());
            entity.setExperienceType(1);
            entity.setStartDate(parseDate(item.getStartDate()));
            entity.setEndDate(parseDate(item.getEndDate()));
            entity.setJobDescription(trimToNull(item.getDescription()));
            entity.setSortOrder(order++);
            workExperienceMapper.insert(entity);
        }
    }

    private void backfillSkills(Long resumeId, List<String> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        skillMapper.delete(new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId));
        int order = 0;
        for (String skill : items) {
            if (!StringUtils.hasText(skill)) {
                continue;
            }
            ResumeSkill entity = new ResumeSkill();
            entity.setResumeId(resumeId);
            entity.setSkillName(skill.trim());
            entity.setSortOrder(order++);
            skillMapper.insert(entity);
        }
    }

    private void backfillProjects(Long resumeId, List<ParseResultBackfillRequest.ProjectItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        projectMapper.delete(new LambdaQueryWrapper<ResumeProject>().eq(ResumeProject::getResumeId, resumeId));
        int order = 0;
        for (ParseResultBackfillRequest.ProjectItem item : items) {
            if (item == null || !StringUtils.hasText(item.getName())) {
                continue;
            }
            ResumeProject entity = new ResumeProject();
            entity.setResumeId(resumeId);
            entity.setProjectName(item.getName().trim());
            entity.setRole(trimToNull(item.getRole()));
            entity.setDescription(trimToNull(item.getDescription()));
            entity.setSortOrder(order++);
            projectMapper.insert(entity);
        }
    }

    private void backfillCertificates(Long resumeId, List<String> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        certificateMapper.delete(
                new LambdaQueryWrapper<ResumeCertificate>().eq(ResumeCertificate::getResumeId, resumeId));
        int order = 0;
        for (String cert : items) {
            if (!StringUtils.hasText(cert)) {
                continue;
            }
            ResumeCertificate entity = new ResumeCertificate();
            entity.setResumeId(resumeId);
            entity.setCertType(1);
            entity.setName(cert.trim());
            entity.setSortOrder(order++);
            certificateMapper.insert(entity);
        }
    }

    private void patchCandidateProfile(Long candidateId, ParseResultBackfillRequest request) {
        if (candidateId == null) {
            return;
        }
        ParseProfilePatchRequest patch = new ParseProfilePatchRequest();
        patch.setCandidateId(candidateId);
        if (request.getBasicInfo() != null) {
            ParseResultBackfillRequest.BasicInfo basic = request.getBasicInfo();
            patch.setRealName(trimToNull(basic.getName()));
            patch.setPhone(trimToNull(basic.getPhone()));
            patch.setEmail(trimToNull(basic.getEmail()));
            patch.setCity(trimToNull(basic.getCity()));
        }
        patch.setCurrentTitle(trimToNull(request.getTargetPosition()));
        patch.setWorkYears(request.getTotalYears());
        patch.setHighestEdu(resolveHighestEdu(request));

        if (!hasProfilePatch(patch)) {
            return;
        }
        try {
            Map<String, Object> res = authFeignClient.patchProfileFromParse(patch);
            Object code = res != null ? res.get("code") : null;
            if (code instanceof Number n && n.intValue() != 200) {
                log.warn(
                        "候选人档案回填失败 candidateId={} msg={}",
                        candidateId,
                        res != null ? res.get("msg") : "null");
            }
        } catch (Exception e) {
            log.warn("候选人档案回填异常 candidateId={} reason={}", candidateId, e.getMessage());
        }
    }

    private boolean hasProfilePatch(ParseProfilePatchRequest patch) {
        return StringUtils.hasText(patch.getRealName())
                || StringUtils.hasText(patch.getPhone())
                || StringUtils.hasText(patch.getEmail())
                || StringUtils.hasText(patch.getCity())
                || StringUtils.hasText(patch.getCurrentTitle())
                || patch.getHighestEdu() != null
                || patch.getWorkYears() != null;
    }

    private Byte resolveHighestEdu(ParseResultBackfillRequest request) {
        if (CollectionUtils.isEmpty(request.getEducation())) {
            return null;
        }
        int max = 0;
        for (ParseResultBackfillRequest.EducationItem item : request.getEducation()) {
            if (item == null) {
                continue;
            }
            Integer degree = parseDegree(item.getDegree());
            if (degree != null && degree > max) {
                max = degree;
            }
        }
        return max > 0 ? (byte) max : null;
    }

    private Integer parseDegree(String degree) {
        if (!StringUtils.hasText(degree)) {
            return null;
        }
        String d = degree.trim().toLowerCase(Locale.ROOT);
        if (d.contains("博士")) {
            return 4;
        }
        if (d.contains("硕士") || d.contains("研究生")) {
            return 3;
        }
        if (d.contains("本科") || d.contains("学士")) {
            return 2;
        }
        if (d.contains("大专") || d.contains("专科")) {
            return 1;
        }
        try {
            return Integer.parseInt(d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.length() >= 10) {
                return LocalDate.parse(trimmed.substring(0, 10), DATE_FMT);
            }
            if (trimmed.length() >= 7) {
                return LocalDate.parse(trimmed.substring(0, 7) + "-01", DATE_FMT);
            }
        } catch (DateTimeParseException ignored) {
            return null;
        }
        return null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
