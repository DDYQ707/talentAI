package com.talent.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.resume.dto.OnlineResumeEducationDTO;
import com.talent.resume.dto.OnlineResumeSaveRequest;
import com.talent.resume.dto.OnlineResumeSkillDTO;
import com.talent.resume.dto.OnlineResumeWorkDTO;
import com.talent.resume.entity.Resume;
import com.talent.resume.entity.ResumeAttachment;
import com.talent.resume.entity.ResumeEducation;
import com.talent.resume.entity.ResumeSkill;
import com.talent.resume.entity.ResumeWorkExperience;
import com.talent.resume.mapper.ResumeAttachmentMapper;
import com.talent.resume.mapper.ResumeEducationMapper;
import com.talent.resume.mapper.ResumeMapper;
import com.talent.resume.mapper.ResumeSkillMapper;
import com.talent.resume.mapper.ResumeWorkExperienceMapper;
import com.talent.resume.vo.OnlineResumeDetailVO;
import com.talent.resume.vo.OnlineResumeListVO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    public List<OnlineResumeListVO> listMyOnlineResumes(Long candidateId) {
        List<Resume> resumes = resumeMapper.selectList(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getCandidateId, candidateId)
                        .orderByDesc(Resume::getUpdatedAt));
        if (resumes.isEmpty()) {
            return List.of();
        }
        Set<Long> withAttachment = loadResumeIdsWithAttachment(
                resumes.stream().map(Resume::getId).toList());

        List<OnlineResumeListVO> result = new ArrayList<>();
        for (Resume resume : resumes) {
            if (withAttachment.contains(resume.getId())) {
                continue;
            }
            OnlineResumeListVO vo = new OnlineResumeListVO();
            vo.setId(resume.getId());
            vo.setResumeName(resume.getResumeName());
            vo.setSummary(resume.getSummary());
            vo.setIsDefault(resume.getIsDefault());
            vo.setUpdatedAt(resume.getUpdatedAt());
            vo.setCompleteness(calcCompleteness(resume.getId()));
            result.add(vo);
        }
        return result;
    }

    public OnlineResumeDetailVO getDetail(Long candidateId, Long resumeId) {
        Resume resume = requireOnlineResume(candidateId, resumeId);
        return buildDetailVo(resume);
    }

    @Transactional(rollbackFor = Exception.class)
    public OnlineResumeDetailVO create(Long candidateId, OnlineResumeSaveRequest request) {
        Resume resume = new Resume();
        resume.setCandidateId(candidateId);
        resume.setResumeName(resolveResumeName(request, "我的在线简历"));
        resume.setSummary(trimToNull(request.getSummary()));
        resume.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        resume.setParseStatus(0);
        resume.setScreenStatus(1);
        resumeMapper.insert(resume);
        saveChildren(resume.getId(), request);
        return buildDetailVo(resumeMapper.selectById(resume.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public OnlineResumeDetailVO update(Long candidateId, Long resumeId, OnlineResumeSaveRequest request) {
        Resume resume = requireOnlineResume(candidateId, resumeId);
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
        return buildDetailVo(resumeMapper.selectById(resumeId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long candidateId, Long resumeId) {
        requireOnlineResume(candidateId, resumeId);
        resumeMapper.deleteById(resumeId);
        educationMapper.delete(new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resumeId));
        workExperienceMapper.delete(
                new LambdaQueryWrapper<ResumeWorkExperience>().eq(ResumeWorkExperience::getResumeId, resumeId));
        skillMapper.delete(new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId));
    }

    private Resume requireOnlineResume(Long candidateId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !candidateId.equals(resume.getCandidateId())) {
            throw new IllegalArgumentException("在线简历不存在或无权访问");
        }
        if (hasAttachment(resumeId)) {
            throw new IllegalArgumentException("该简历为附件简历，请使用附件管理");
        }
        return resume;
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
        saveChildren(resumeId, request);
    }

    private void saveChildren(Long resumeId, OnlineResumeSaveRequest request) {
        if (request == null) {
            return;
        }
        saveEducations(resumeId, request.getEducations());
        saveWorks(resumeId, request.getWorkExperiences());
        saveSkills(resumeId, request.getSkills());
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
            if (!StringUtils.hasText(dto.getCompanyName()) || !StringUtils.hasText(dto.getJobTitle())) {
                continue;
            }
            ResumeWorkExperience entity = new ResumeWorkExperience();
            entity.setResumeId(resumeId);
            entity.setCompanyName(dto.getCompanyName().trim());
            entity.setJobTitle(dto.getJobTitle().trim());
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

    private OnlineResumeDetailVO buildDetailVo(Resume resume) {
        Long resumeId = resume.getId();
        OnlineResumeDetailVO vo = new OnlineResumeDetailVO();
        vo.setId(resumeId);
        vo.setResumeName(resume.getResumeName());
        vo.setSummary(resume.getSummary());
        vo.setIsDefault(resume.getIsDefault());
        vo.setCreatedAt(resume.getCreatedAt());
        vo.setUpdatedAt(resume.getUpdatedAt());
        vo.setCompleteness(calcCompleteness(resumeId));

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

    private int calcCompleteness(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            return 0;
        }
        int score = 0;
        if (StringUtils.hasText(resume.getResumeName())) {
            score += 15;
        }
        if (StringUtils.hasText(resume.getSummary())) {
            score += 15;
        }
        Long edu = educationMapper.selectCount(
                new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resumeId));
        if (edu != null && edu > 0) {
            score += 25;
        }
        Long work = workExperienceMapper.selectCount(
                new LambdaQueryWrapper<ResumeWorkExperience>().eq(ResumeWorkExperience::getResumeId, resumeId));
        if (work != null && work > 0) {
            score += 25;
        }
        Long skill = skillMapper.selectCount(
                new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId));
        if (skill != null && skill > 0) {
            score += 20;
        }
        return Math.min(100, score);
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
