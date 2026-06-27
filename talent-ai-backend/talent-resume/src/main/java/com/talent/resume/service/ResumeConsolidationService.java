package com.talent.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.talent.resume.entity.Resume;
import com.talent.resume.entity.ResumeAttachment;
import com.talent.resume.entity.ResumeCertificate;
import com.talent.resume.entity.ResumeEducation;
import com.talent.resume.entity.ResumeProject;
import com.talent.resume.entity.ResumeSkill;
import com.talent.resume.entity.ResumeWorkExperience;
import com.talent.resume.mapper.ResumeAttachmentMapper;
import com.talent.resume.mapper.ResumeCertificateMapper;
import com.talent.resume.mapper.ResumeEducationMapper;
import com.talent.resume.mapper.ResumeMapper;
import com.talent.resume.mapper.ResumeProjectMapper;
import com.talent.resume.mapper.ResumeSkillMapper;
import com.talent.resume.mapper.ResumeWorkExperienceMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 保证每个候选人账号仅保留一份有效简历；历史重复记录在写入时合并清理。
 */
@Service
@RequiredArgsConstructor
public class ResumeConsolidationService {

    private final ResumeMapper resumeMapper;
    private final ResumeAttachmentMapper attachmentMapper;
    private final ResumeEducationMapper educationMapper;
    private final ResumeWorkExperienceMapper workExperienceMapper;
    private final ResumeSkillMapper skillMapper;
    private final ResumeProjectMapper projectMapper;
    private final ResumeCertificateMapper certificateMapper;

    /** HR 列表加载前：持久化合并库中所有重复简历 */
    @Transactional(rollbackFor = Exception.class)
    public int consolidateAllDuplicateCandidatesInDatabase() {
        List<Resume> all = resumeMapper.selectList(
                new LambdaQueryWrapper<Resume>().orderByDesc(Resume::getUpdatedAt).orderByDesc(Resume::getId));
        Map<Long, List<Resume>> grouped = all.stream().collect(Collectors.groupingBy(Resume::getCandidateId));
        int removed = 0;
        for (Map.Entry<Long, List<Resume>> entry : grouped.entrySet()) {
            List<Resume> list = entry.getValue();
            if (list == null || list.size() <= 1) {
                continue;
            }
            Resume keep = pickPreferredResume(list);
            int before = list.size();
            consolidateCandidateResumes(entry.getKey(), keep.getId());
            removed += before - 1;
        }
        return removed;
    }

    /** 获取候选人当前唯一简历（若有多份则先合并） */
    @Transactional(rollbackFor = Exception.class)
    public Resume getPrimaryResume(Long candidateId) {
        if (candidateId == null) {
            return null;
        }
        List<Resume> all = listCandidateResumes(candidateId);
        if (all.isEmpty()) {
            return null;
        }
        if (all.size() == 1) {
            return all.get(0);
        }
        Resume keep = pickPreferredResume(all);
        consolidateCandidateResumes(candidateId, keep.getId());
        return resumeMapper.selectById(keep.getId());
    }

    /** 获取或创建候选人的唯一简历主记录 */
    @Transactional(rollbackFor = Exception.class)
    public Resume getOrCreatePrimaryResume(Long candidateId) {
        Resume primary = getPrimaryResume(candidateId);
        if (primary != null) {
            return primary;
        }
        Resume resume = newResumeShell(candidateId, "我的简历");
        resumeMapper.insert(resume);
        return resume;
    }

    /** 合并同一候选人的重复简历，仅保留 keepResumeId，并合并附件/在线内容 */
    @Transactional(rollbackFor = Exception.class)
    public void consolidateCandidateResumes(Long candidateId, Long keepResumeId) {
        List<Resume> all = listCandidateResumes(candidateId);
        Resume keep = resumeMapper.selectById(keepResumeId);
        if (keep == null) {
            return;
        }
        for (Resume resume : all) {
            if (keepResumeId.equals(resume.getId())) {
                continue;
            }
            mergeContentInto(resume.getId(), keepResumeId);
            clearOnlineContent(resume.getId());
            attachmentMapper.delete(
                    new LambdaQueryWrapper<ResumeAttachment>().eq(ResumeAttachment::getResumeId, resume.getId()));
            resumeMapper.deleteById(resume.getId());
        }
    }

    /** HR 列表：每个候选人只保留一条（最新；同时间优先有附件可预览的） */
    public List<Resume> dedupeForHrList(List<Resume> resumes, Map<Long, ResumeAttachment> latestAttachments) {
        if (resumes == null || resumes.isEmpty()) {
            return List.of();
        }
        Map<Long, Resume> bestByCandidate = new LinkedHashMap<>();
        for (Resume resume : resumes) {
            Long candidateId = resume.getCandidateId();
            Resume existing = bestByCandidate.get(candidateId);
            if (existing == null) {
                bestByCandidate.put(candidateId, resume);
                continue;
            }
            if (compareForHr(existing, latestAttachments.get(existing.getId()), resume, latestAttachments.get(resume.getId())) < 0) {
                bestByCandidate.put(candidateId, resume);
            }
        }
        return new ArrayList<>(bestByCandidate.values());
    }

    @Transactional(rollbackFor = Exception.class)
    public void clearOnlineContent(Long resumeId) {
        educationMapper.delete(new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resumeId));
        workExperienceMapper.delete(
                new LambdaQueryWrapper<ResumeWorkExperience>().eq(ResumeWorkExperience::getResumeId, resumeId));
        skillMapper.delete(new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId));
        projectMapper.delete(new LambdaQueryWrapper<ResumeProject>().eq(ResumeProject::getResumeId, resumeId));
        certificateMapper.delete(
                new LambdaQueryWrapper<ResumeCertificate>().eq(ResumeCertificate::getResumeId, resumeId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void clearAttachments(Long resumeId) {
        attachmentMapper.delete(new LambdaQueryWrapper<ResumeAttachment>().eq(ResumeAttachment::getResumeId, resumeId));
    }

    public boolean hasAttachment(Long resumeId) {
        Long count = attachmentMapper.selectCount(
                new LambdaQueryWrapper<ResumeAttachment>().eq(ResumeAttachment::getResumeId, resumeId));
        return count != null && count > 0;
    }

    public Resume newResumeShell(Long candidateId, String resumeName) {
        Resume resume = new Resume();
        resume.setCandidateId(candidateId);
        resume.setResumeName(resumeName);
        resume.setIsDefault(1);
        resume.setParseStatus(0);
        resume.setScreenStatus(1);
        return resume;
    }

    /** 将 from 的附件/在线内容合并到 keep，避免删除重复记录时丢失数据 */
    private void mergeContentInto(Long fromResumeId, Long keepResumeId) {
        if (fromResumeId == null || keepResumeId == null || fromResumeId.equals(keepResumeId)) {
            return;
        }
        if (!hasAttachment(keepResumeId) && hasAttachment(fromResumeId)) {
            attachmentMapper.update(
                    null,
                    new LambdaUpdateWrapper<ResumeAttachment>()
                            .eq(ResumeAttachment::getResumeId, fromResumeId)
                            .set(ResumeAttachment::getResumeId, keepResumeId));
        }
        mergeOnlineContentPreferRicher(fromResumeId, keepResumeId);

        Resume from = resumeMapper.selectById(fromResumeId);
        Resume keep = resumeMapper.selectById(keepResumeId);
        if (from == null || keep == null) {
            return;
        }
        boolean changed = false;
        if (!StringUtils.hasText(keep.getSummary()) && StringUtils.hasText(from.getSummary())) {
            keep.setSummary(from.getSummary());
            changed = true;
        }
        if (!StringUtils.hasText(keep.getResumeName()) && StringUtils.hasText(from.getResumeName())) {
            keep.setResumeName(from.getResumeName());
            changed = true;
        }
        if (changed) {
            resumeMapper.updateById(keep);
        }
    }

    /** 将更完整的在线简历内容合并到保留记录（附件简历常无子表数据） */
    private void mergeOnlineContentPreferRicher(Long fromResumeId, Long keepResumeId) {
        long fromScore = countEducations(fromResumeId) + countWorks(fromResumeId) + countSkills(fromResumeId)
                + countProjects(fromResumeId) + countCertificates(fromResumeId);
        long keepScore = countEducations(keepResumeId) + countWorks(keepResumeId) + countSkills(keepResumeId)
                + countProjects(keepResumeId) + countCertificates(keepResumeId);
        if (fromScore <= keepScore) {
            copyOnlineContentIfEmpty(fromResumeId, keepResumeId);
            return;
        }
        clearOnlineContent(keepResumeId);
        copyEducations(fromResumeId, keepResumeId);
        copyWorks(fromResumeId, keepResumeId);
        copySkills(fromResumeId, keepResumeId);
        copyProjects(fromResumeId, keepResumeId);
        copyCertificates(fromResumeId, keepResumeId);
    }

    private void copyOnlineContentIfEmpty(Long fromResumeId, Long keepResumeId) {
        if (countEducations(keepResumeId) == 0 && countEducations(fromResumeId) > 0) {
            copyEducations(fromResumeId, keepResumeId);
        }
        if (countWorks(keepResumeId) == 0 && countWorks(fromResumeId) > 0) {
            copyWorks(fromResumeId, keepResumeId);
        }
        if (countSkills(keepResumeId) == 0 && countSkills(fromResumeId) > 0) {
            copySkills(fromResumeId, keepResumeId);
        }
        if (countProjects(keepResumeId) == 0 && countProjects(fromResumeId) > 0) {
            copyProjects(fromResumeId, keepResumeId);
        }
        if (countCertificates(keepResumeId) == 0 && countCertificates(fromResumeId) > 0) {
            copyCertificates(fromResumeId, keepResumeId);
        }
    }

    private void copyEducations(Long fromResumeId, Long keepResumeId) {
        List<ResumeEducation> list = educationMapper.selectList(
                new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, fromResumeId));
        for (ResumeEducation src : list) {
            ResumeEducation copy = new ResumeEducation();
            copy.setResumeId(keepResumeId);
            copy.setSchoolName(src.getSchoolName());
            copy.setMajor(src.getMajor());
            copy.setDegree(src.getDegree());
            copy.setStartDate(src.getStartDate());
            copy.setEndDate(src.getEndDate());
            copy.setDescription(src.getDescription());
            copy.setSortOrder(src.getSortOrder());
            educationMapper.insert(copy);
        }
    }

    private void copyWorks(Long fromResumeId, Long keepResumeId) {
        List<ResumeWorkExperience> list = workExperienceMapper.selectList(
                new LambdaQueryWrapper<ResumeWorkExperience>().eq(ResumeWorkExperience::getResumeId, fromResumeId));
        for (ResumeWorkExperience src : list) {
            ResumeWorkExperience copy = new ResumeWorkExperience();
            copy.setResumeId(keepResumeId);
            copy.setCompanyName(src.getCompanyName());
            copy.setJobTitle(src.getJobTitle());
            copy.setExperienceType(src.getExperienceType());
            copy.setStartDate(src.getStartDate());
            copy.setEndDate(src.getEndDate());
            copy.setJobDescription(src.getJobDescription());
            copy.setSortOrder(src.getSortOrder());
            workExperienceMapper.insert(copy);
        }
    }

    private void copySkills(Long fromResumeId, Long keepResumeId) {
        List<ResumeSkill> list = skillMapper.selectList(
                new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, fromResumeId));
        for (ResumeSkill src : list) {
            ResumeSkill copy = new ResumeSkill();
            copy.setResumeId(keepResumeId);
            copy.setSkillName(src.getSkillName());
            copy.setProficiencyLevel(src.getProficiencyLevel());
            copy.setSortOrder(src.getSortOrder());
            skillMapper.insert(copy);
        }
    }

    private void copyProjects(Long fromResumeId, Long keepResumeId) {
        List<ResumeProject> list = projectMapper.selectList(
                new LambdaQueryWrapper<ResumeProject>().eq(ResumeProject::getResumeId, fromResumeId));
        for (ResumeProject src : list) {
            ResumeProject copy = new ResumeProject();
            copy.setResumeId(keepResumeId);
            copy.setProjectName(src.getProjectName());
            copy.setRole(src.getRole());
            copy.setTechStack(src.getTechStack());
            copy.setStartDate(src.getStartDate());
            copy.setEndDate(src.getEndDate());
            copy.setDescription(src.getDescription());
            copy.setLinkUrl(src.getLinkUrl());
            copy.setSortOrder(src.getSortOrder());
            projectMapper.insert(copy);
        }
    }

    private void copyCertificates(Long fromResumeId, Long keepResumeId) {
        List<ResumeCertificate> list = certificateMapper.selectList(
                new LambdaQueryWrapper<ResumeCertificate>().eq(ResumeCertificate::getResumeId, fromResumeId));
        for (ResumeCertificate src : list) {
            ResumeCertificate copy = new ResumeCertificate();
            copy.setResumeId(keepResumeId);
            copy.setCertType(src.getCertType());
            copy.setName(src.getName());
            copy.setIssuer(src.getIssuer());
            copy.setIssueDate(src.getIssueDate());
            copy.setDescription(src.getDescription());
            copy.setSortOrder(src.getSortOrder());
            certificateMapper.insert(copy);
        }
    }

    private long countEducations(Long resumeId) {
        Long c = educationMapper.selectCount(
                new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resumeId));
        return c == null ? 0 : c;
    }

    private long countWorks(Long resumeId) {
        Long c = workExperienceMapper.selectCount(
                new LambdaQueryWrapper<ResumeWorkExperience>().eq(ResumeWorkExperience::getResumeId, resumeId));
        return c == null ? 0 : c;
    }

    private long countSkills(Long resumeId) {
        Long c = skillMapper.selectCount(
                new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId));
        return c == null ? 0 : c;
    }

    private long countProjects(Long resumeId) {
        Long c = projectMapper.selectCount(
                new LambdaQueryWrapper<ResumeProject>().eq(ResumeProject::getResumeId, resumeId));
        return c == null ? 0 : c;
    }

    private long countCertificates(Long resumeId) {
        Long c = certificateMapper.selectCount(
                new LambdaQueryWrapper<ResumeCertificate>().eq(ResumeCertificate::getResumeId, resumeId));
        return c == null ? 0 : c;
    }

    private List<Resume> listCandidateResumes(Long candidateId) {
        return resumeMapper.selectList(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getCandidateId, candidateId)
                        .orderByDesc(Resume::getUpdatedAt)
                        .orderByDesc(Resume::getId));
    }

    private Resume pickPreferredResume(List<Resume> resumes) {
        Map<Long, ResumeAttachment> attMap = loadLatestAttachments(
                resumes.stream().map(Resume::getId).toList());
        return resumes.stream()
                .max(Comparator.comparingInt((Resume r) -> scoreForPick(r, attMap.get(r.getId())))
                        .thenComparing(Resume::getId))
                .orElse(resumes.get(0));
    }

    private int scoreForPick(Resume resume, ResumeAttachment attachment) {
        long updated = 0;
        if (resume.getUpdatedAt() != null) {
            updated = resume.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
        }
        int score = (int) Math.min(Integer.MAX_VALUE / 2, updated / 60);
        if (attachment != null) {
            score += 1_000_000_000;
        }
        return score;
    }

    private int compareForHr(Resume a, ResumeAttachment attA, Resume b, ResumeAttachment attB) {
        return Integer.compare(scoreForPick(a, attA), scoreForPick(b, attB));
    }

    private Map<Long, ResumeAttachment> loadLatestAttachments(List<Long> resumeIds) {
        if (resumeIds == null || resumeIds.isEmpty()) {
            return Map.of();
        }
        List<ResumeAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<ResumeAttachment>()
                        .in(ResumeAttachment::getResumeId, resumeIds)
                        .orderByDesc(ResumeAttachment::getCreatedAt));
        Map<Long, ResumeAttachment> latestByResume = new HashMap<>();
        for (ResumeAttachment att : attachments) {
            latestByResume.putIfAbsent(att.getResumeId(), att);
        }
        return latestByResume;
    }
}
