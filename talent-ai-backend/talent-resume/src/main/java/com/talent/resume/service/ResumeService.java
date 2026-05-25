package com.talent.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.resume.constant.ResumeConstants;
import com.talent.resume.entity.Resume;
import com.talent.resume.entity.ResumeAttachment;
import com.talent.resume.entity.ResumeEducation;
import com.talent.resume.entity.ResumeSkill;
import com.talent.resume.entity.ResumeWorkExperience;
import com.talent.resume.feign.AuthFeignClient;
import com.talent.resume.feign.JobFeignClient;
import com.talent.resume.mapper.ResumeAttachmentMapper;
import com.talent.resume.mapper.ResumeEducationMapper;
import com.talent.resume.mapper.ResumeMapper;
import com.talent.resume.mapper.ResumeSkillMapper;
import com.talent.resume.mapper.ResumeWorkExperienceMapper;
import com.talent.resume.service.MinioStorageService.StoredObject;
import com.talent.resume.vo.HrResumeDetailVO;
import com.talent.resume.vo.HrResumeListVO;
import com.talent.resume.vo.ResumeListVO;
import com.talent.resume.vo.ResumePreviewVO;
import com.talent.resume.vo.ResumeUploadVO;
import com.talent.resume.dto.OnlineResumeEducationDTO;
import com.talent.resume.dto.OnlineResumeSkillDTO;
import com.talent.resume.dto.OnlineResumeWorkDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ResumeMapper resumeMapper;
    private final ResumeAttachmentMapper attachmentMapper;
    private final ResumeEducationMapper educationMapper;
    private final ResumeWorkExperienceMapper workExperienceMapper;
    private final ResumeSkillMapper skillMapper;
    private final MinioStorageService minioStorageService;
    private final AuthFeignClient authFeignClient;
    private final JobFeignClient jobFeignClient;
    private final ResumeConsolidationService consolidationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 仅返回已上传附件的简历（用于投递选择，一账号一条） */
    public List<ResumeListVO> listAttachmentResumes(Long candidateId) {
        Resume primary = consolidationService.getPrimaryResume(candidateId);
        if (primary == null) {
            return List.of();
        }
        ResumeAttachment att = getLatestAttachment(primary.getId());
        if (att == null) {
            return List.of();
        }
        return List.of(toListVo(primary, att));
    }

    public List<ResumeListVO> listMyResumes(Long candidateId) {
        return listAttachmentResumes(candidateId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResumeUploadVO uploadResumeFile(
            Long candidateId, MultipartFile file, Long resumeId, String resumeName) throws Exception {
        validateFile(file);

        Resume resume;
        if (resumeId != null) {
            resume = resumeMapper.selectById(resumeId);
            if (resume == null || !candidateId.equals(resume.getCandidateId())) {
                throw new IllegalArgumentException("简历不存在或无权访问");
            }
        } else {
            resume = consolidationService.getOrCreatePrimaryResume(candidateId);
        }

        consolidationService.clearAttachments(resume.getId());

        resume.setResumeName(resolveResumeName(file, resumeName));
        resume.setScreenStatus(1);
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);

        StoredObject stored = minioStorageService.upload(file, candidateId);
        String ext = extractExtension(file.getOriginalFilename());

        ResumeAttachment attachment = new ResumeAttachment();
        attachment.setResumeId(resume.getId());
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(ext);
        attachment.setFileSize(file.getSize());
        attachment.setBucketName(stored.bucketName());
        attachment.setObjectKey(stored.objectKey());
        attachment.setFileUrl(stored.fileUrl());
        attachment.setUploadStatus(ResumeConstants.UPLOAD_STATUS_SUCCESS);
        attachmentMapper.insert(attachment);

        consolidationService.consolidateCandidateResumes(candidateId, resume.getId());

        ResumeUploadVO vo = new ResumeUploadVO();
        vo.setResumeId(resume.getId());
        vo.setAttachmentId(attachment.getId());
        vo.setResumeName(resume.getResumeName());
        vo.setFileName(attachment.getFileName());
        vo.setFileType(attachment.getFileType());
        vo.setFileSize(attachment.getFileSize());
        vo.setFileUrl(attachment.getFileUrl());
        vo.setObjectKey(attachment.getObjectKey());
        return vo;
    }

    public ResumePreviewVO getPreviewByAttachmentId(Long userId, String role, Long attachmentId) throws Exception {
        ResumeAttachment att = requireAttachment(attachmentId);
        assertCanAccessAttachment(userId, role, att);
        return buildPreviewVo(att);
    }

    public ResumePreviewVO getPreviewByResumeId(Long userId, String role, Long resumeId) throws Exception {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new IllegalArgumentException("简历不存在");
        }
        assertCanAccessResume(userId, role, resume);
        ResumeAttachment att = getLatestAttachment(resumeId);
        if (att == null) {
            throw new IllegalArgumentException("该简历暂无附件");
        }
        return buildPreviewVo(att);
    }

    /** 兼容旧下载接口：返回预签名 URL 字符串 */
    public String getDownloadUrl(Long userId, String role, Long attachmentId) throws Exception {
        return getPreviewByAttachmentId(userId, role, attachmentId).getPresignedUrl();
    }

    public Map<String, Object> pageHrResumes(
            String role, Integer current, Integer size, String keyword, Integer screenStatus) {
        assertHrRole(role);

        try {
            consolidationService.consolidateAllDuplicateCandidatesInDatabase();
        } catch (Exception e) {
            // 合并失败时仍返回去重列表，避免整页不可用
        }

        int pageNum = current != null && current > 0 ? current : 1;
        int pageSize = size != null && size > 0 ? Math.min(size, 50) : 20;

        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        if (screenStatus != null) {
            wrapper.eq(Resume::getScreenStatus, screenStatus);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Resume::getResumeName, keyword.trim());
        }
        wrapper.orderByDesc(Resume::getUpdatedAt);

        List<Resume> allMatching = resumeMapper.selectList(wrapper);
        List<Long> allIds = allMatching.stream().map(Resume::getId).toList();
        Map<Long, ResumeAttachment> allAttMap = loadLatestAttachments(allIds);
        List<Resume> deduped = consolidationService.dedupeForHrList(allMatching, allAttMap);
        deduped.sort((a, b) -> {
            if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) {
                return Long.compare(b.getId(), a.getId());
            }
            if (a.getUpdatedAt() == null) {
                return 1;
            }
            if (b.getUpdatedAt() == null) {
                return -1;
            }
            int cmp = b.getUpdatedAt().compareTo(a.getUpdatedAt());
            return cmp != 0 ? cmp : Long.compare(b.getId(), a.getId());
        });

        int total = deduped.size();
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        int from = Math.min((pageNum - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<Resume> pageRecords = deduped.subList(from, to);

        List<Long> resumeIds = pageRecords.stream().map(Resume::getId).toList();
        Map<Long, ResumeAttachment> latestMap = loadLatestAttachments(resumeIds);

        List<HrResumeListVO> records = new ArrayList<>();
        for (Resume resume : pageRecords) {
            HrResumeListVO vo = new HrResumeListVO();
            vo.setId(resume.getId());
            vo.setCandidateId(resume.getCandidateId());
            vo.setResumeName(resume.getResumeName());
            vo.setScreenStatus(resume.getScreenStatus());
            vo.setParseStatus(resume.getParseStatus());
            vo.setUpdatedAt(resume.getUpdatedAt());
            ResumeAttachment att = latestMap.get(resume.getId());
            if (att != null) {
                vo.setResumeType("attachment");
                vo.setAttachmentId(att.getId());
                vo.setFileName(att.getFileName());
                vo.setFileType(att.getFileType());
                vo.setFileSize(att.getFileSize());
            } else {
                vo.setResumeType("online");
            }
            applyCandidateProfileToListVo(vo, resume.getCandidateId());
            records.add(vo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("current", pageNum);
        data.put("pages", pages);
        data.put("records", records);
        return data;
    }

    public HrResumeDetailVO getHrResumeDetail(String role, Long resumeId) {
        assertHrRole(role);
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new IllegalArgumentException("简历不存在");
        }
        Resume primary = consolidationService.getPrimaryResume(resume.getCandidateId());
        if (primary == null) {
            throw new IllegalArgumentException("简历不存在");
        }
        resumeId = primary.getId();
        resume = primary;

        HrResumeDetailVO vo = new HrResumeDetailVO();
        vo.setId(resume.getId());
        vo.setCandidateId(resume.getCandidateId());
        vo.setResumeName(resume.getResumeName());
        vo.setSummary(resume.getSummary());
        vo.setIsDefault(resume.getIsDefault());
        vo.setParseStatus(resume.getParseStatus());
        vo.setScreenStatus(resume.getScreenStatus());
        vo.setCreatedAt(resume.getCreatedAt());
        vo.setUpdatedAt(resume.getUpdatedAt());

        ResumeAttachment att = getLatestAttachment(resumeId);
        if (att != null) {
            vo.setResumeType("attachment");
            vo.setAttachmentId(att.getId());
            vo.setFileName(att.getFileName());
            vo.setFileType(att.getFileType());
            vo.setFileSize(att.getFileSize());
        } else {
            vo.setResumeType("online");
        }

        fillCandidateProfile(vo, resume.getCandidateId());
        fillOnlineContentAggregated(vo, resume.getCandidateId());
        fillLatestApplicationForCandidate(vo, resume.getCandidateId());

        if (!StringUtils.hasText(vo.getCandidateName())) {
            vo.setCandidateName(resolveCandidateName(resume.getCandidateId()));
        }
        return vo;
    }

    private void fillCandidateProfile(HrResumeDetailVO vo, Long candidateId) {
        Map<String, Object> brief = loadCandidateProfileBrief(candidateId);
        if (brief.isEmpty()) {
            return;
        }
        Object realName = brief.get("realName");
        if (realName instanceof String s && StringUtils.hasText(s)) {
            vo.setCandidateName(s.trim());
        }
        if (brief.get("phone") instanceof String phone) {
            vo.setPhone(phone);
        }
        if (brief.get("email") instanceof String email) {
            vo.setEmail(email);
        }
        if (brief.get("city") instanceof String city) {
            vo.setCity(city);
        }
        if (brief.get("currentTitle") instanceof String title) {
            vo.setCurrentTitle(title);
        }
        if (brief.get("highestEdu") instanceof Number edu) {
            vo.setHighestEdu(edu.byteValue());
        }
        if (brief.get("aiScore") instanceof Number score) {
            vo.setMatchScore(score.byteValue());
        }
    }

    private void applyCandidateProfileToListVo(HrResumeListVO vo, Long candidateId) {
        Map<String, Object> brief = loadCandidateProfileBrief(candidateId);
        if (brief.isEmpty()) {
            vo.setCandidateName(resolveCandidateName(candidateId));
            return;
        }
        Object realName = brief.get("realName");
        if (realName instanceof String s && StringUtils.hasText(s)) {
            vo.setCandidateName(s.trim());
        } else {
            vo.setCandidateName(resolveCandidateName(candidateId));
        }
        if (brief.get("phone") instanceof String phone) {
            vo.setPhone(phone);
        }
        if (brief.get("city") instanceof String city) {
            vo.setCity(city);
        }
        if (brief.get("currentTitle") instanceof String title) {
            vo.setCurrentTitle(title);
        }
        if (brief.get("highestEdu") instanceof Number edu) {
            vo.setHighestEdu(edu.byteValue());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadCandidateProfileBrief(Long candidateId) {
        if (candidateId == null) {
            return Map.of();
        }
        try {
            Map<String, Object> res = authFeignClient.getCandidateProfileBrief(candidateId);
            if (res == null || res.isEmpty()) {
                return Map.of();
            }
            if (res.containsKey("code") && res.get("data") instanceof Map<?, ?> data) {
                return (Map<String, Object>) data;
            }
            return res;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 聚合该候选人名下所有简历子表（解决附件简历 id 与在线简历 id 不一致导致详情空白）。
     */
    private void fillOnlineContentAggregated(HrResumeDetailVO vo, Long candidateId) {
        List<Resume> resumes = resumeMapper.selectList(
                new LambdaQueryWrapper<Resume>().eq(Resume::getCandidateId, candidateId));
        if (resumes.isEmpty()) {
            return;
        }
        List<Long> resumeIds = resumes.stream().map(Resume::getId).toList();

        Long richestResumeId = resumeIds.stream()
                .max(Comparator.comparingLong(this::onlineContentScore))
                .orElse(resumeIds.get(0));
        fillOnlineContent(vo, richestResumeId);

        Resume richest = resumeMapper.selectById(richestResumeId);
        if (richest != null && StringUtils.hasText(richest.getSummary())) {
            vo.setSummary(richest.getSummary());
        }

        ResumeAttachment bestAtt = null;
        for (Long rid : resumeIds) {
            ResumeAttachment att = getLatestAttachment(rid);
            if (att == null) {
                continue;
            }
            if (bestAtt == null
                    || (att.getCreatedAt() != null
                            && (bestAtt.getCreatedAt() == null
                                    || att.getCreatedAt().isAfter(bestAtt.getCreatedAt())))) {
                bestAtt = att;
            }
        }
        if (bestAtt != null) {
            vo.setResumeType("attachment");
            vo.setAttachmentId(bestAtt.getId());
            vo.setFileName(bestAtt.getFileName());
            vo.setFileType(bestAtt.getFileType());
            vo.setFileSize(bestAtt.getFileSize());
        }
    }

    private long onlineContentScore(Long resumeId) {
        Long edu = educationMapper.selectCount(
                new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resumeId));
        Long work = workExperienceMapper.selectCount(
                new LambdaQueryWrapper<ResumeWorkExperience>().eq(ResumeWorkExperience::getResumeId, resumeId));
        Long skill = skillMapper.selectCount(
                new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId));
        return (edu == null ? 0 : edu) + (work == null ? 0 : work) + (skill == null ? 0 : skill);
    }

    private void fillOnlineContent(HrResumeDetailVO vo, Long resumeId) {
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
    }

    private void fillLatestApplicationForCandidate(HrResumeDetailVO vo, Long candidateId) {
        try {
            Map<String, Object> app = jobFeignClient.getLatestApplicationByCandidate(candidateId);
            applyApplicationBrief(vo, app);
        } catch (Exception ignored) {
            // job 服务不可用时忽略
        }
    }

    private void applyApplicationBrief(HrResumeDetailVO vo, Map<String, Object> app) {
        if (app == null || app.isEmpty()) {
            return;
        }
        if (app.get("jobTitle") instanceof String title) {
            vo.setAppliedJobTitle(title);
        }
        if (app.get("appliedAt") instanceof LocalDateTime appliedAt) {
            vo.setAppliedAt(appliedAt);
        } else if (app.get("appliedAt") instanceof String appliedAtStr) {
            vo.setAppliedAt(LocalDateTime.parse(appliedAtStr));
        }
        if (app.get("matchScore") instanceof Number score) {
            vo.setMatchScore(score.byteValue());
        }
    }

    private void fillLatestApplication(HrResumeDetailVO vo, Long resumeId) {
        try {
            Map<String, Object> app = jobFeignClient.getLatestApplicationByResume(resumeId);
            applyApplicationBrief(vo, app);
        } catch (Exception ignored) {
            // job 服务不可用时忽略
        }
    }

    public int hrConsolidateDuplicates(String role) {
        assertHrRole(role);
        return consolidationService.consolidateAllDuplicateCandidatesInDatabase();
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

    private String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FMT);
    }

    public ResumeListVO getAttachmentBriefByResumeId(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            return null;
        }
        return toListVo(resume, getLatestAttachment(resumeId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteResume(Long candidateId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !candidateId.equals(resume.getCandidateId())) {
            throw new IllegalArgumentException("简历不存在或无权删除");
        }
        ResumeAttachment att = attachmentMapper.selectOne(
                new LambdaQueryWrapper<ResumeAttachment>()
                        .eq(ResumeAttachment::getResumeId, resumeId)
                        .last("LIMIT 1"));
        if (att == null) {
            throw new IllegalArgumentException("该记录为在线简历，请在在线简历页删除");
        }
        resumeMapper.deleteById(resumeId);
        attachmentMapper.delete(
                new LambdaQueryWrapper<ResumeAttachment>().eq(ResumeAttachment::getResumeId, resumeId));
    }

    private ResumePreviewVO buildPreviewVo(ResumeAttachment att) throws Exception {
        if (!StringUtils.hasText(att.getBucketName()) || !StringUtils.hasText(att.getObjectKey())) {
            throw new IllegalArgumentException("附件存储路径缺失，请重新上传简历");
        }
        String url = minioStorageService.getPresignedPreviewUrl(
                att.getBucketName(),
                att.getObjectKey(),
                att.getFileName(),
                att.getFileType(),
                ResumeConstants.PREVIEW_EXPIRE_HOURS);
        ResumePreviewVO vo = new ResumePreviewVO();
        vo.setAttachmentId(att.getId());
        vo.setResumeId(att.getResumeId());
        vo.setFileName(att.getFileName());
        vo.setFileType(att.getFileType());
        vo.setPresignedUrl(url);
        vo.setExpiresIn(ResumeConstants.PREVIEW_EXPIRE_HOURS * 3600);
        return vo;
    }

    private ResumeAttachment requireAttachment(Long attachmentId) {
        ResumeAttachment att = attachmentMapper.selectById(attachmentId);
        if (att == null) {
            throw new IllegalArgumentException("附件不存在");
        }
        return att;
    }

    private void assertCanAccessAttachment(Long userId, String role, ResumeAttachment att) {
        if (isHrOrAdmin(role)) {
            return;
        }
        Resume resume = resumeMapper.selectById(att.getResumeId());
        if (resume == null || userId == null || !userId.equals(resume.getCandidateId())) {
            throw new IllegalArgumentException("无权访问该附件");
        }
    }

    private void assertCanAccessResume(Long userId, String role, Resume resume) {
        if (isHrOrAdmin(role)) {
            return;
        }
        if (userId == null || !userId.equals(resume.getCandidateId())) {
            throw new IllegalArgumentException("无权访问该简历");
        }
    }

    private boolean isHrOrAdmin(String role) {
        if (role == null) {
            return false;
        }
        String r = role.trim();
        return ResumeConstants.ROLE_HR.equalsIgnoreCase(r) || ResumeConstants.ROLE_ADMIN.equalsIgnoreCase(r);
    }

    private void assertHrRole(String role) {
        if (!isHrOrAdmin(role)) {
            throw new IllegalArgumentException("仅 HR 或管理员可访问");
        }
    }

    private ResumeAttachment getLatestAttachment(Long resumeId) {
        return attachmentMapper.selectOne(
                new LambdaQueryWrapper<ResumeAttachment>()
                        .eq(ResumeAttachment::getResumeId, resumeId)
                        .eq(ResumeAttachment::getUploadStatus, ResumeConstants.UPLOAD_STATUS_SUCCESS)
                        .orderByDesc(ResumeAttachment::getCreatedAt)
                        .last("LIMIT 1"));
    }

    private Map<Long, ResumeAttachment> loadLatestAttachments(List<Long> resumeIds) {
        if (resumeIds == null || resumeIds.isEmpty()) {
            return Map.of();
        }
        List<ResumeAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<ResumeAttachment>()
                        .in(ResumeAttachment::getResumeId, resumeIds)
                        .eq(ResumeAttachment::getUploadStatus, ResumeConstants.UPLOAD_STATUS_SUCCESS)
                        .orderByDesc(ResumeAttachment::getCreatedAt));
        Map<Long, ResumeAttachment> latestByResume = new HashMap<>();
        for (ResumeAttachment att : attachments) {
            latestByResume.putIfAbsent(att.getResumeId(), att);
        }
        return latestByResume;
    }

    private ResumeListVO toListVo(Resume resume, ResumeAttachment att) {
        ResumeListVO vo = new ResumeListVO();
        vo.setId(resume.getId());
        vo.setResumeName(resume.getResumeName());
        vo.setIsDefault(resume.getIsDefault());
        vo.setUpdatedAt(resume.getUpdatedAt());
        if (att != null) {
            vo.setAttachmentId(att.getId());
            vo.setFileName(att.getFileName());
            vo.setFileType(att.getFileType());
            vo.setFileSize(att.getFileSize());
            vo.setFileUrl(att.getFileUrl());
        }
        return vo;
    }

    private String resolveCandidateName(Long candidateId) {
        if (candidateId == null) {
            return "未知候选人";
        }
        try {
            String name = authFeignClient.getUserName(candidateId);
            return StringUtils.hasText(name) ? name : "候选人" + candidateId;
        } catch (Exception e) {
            return "候选人" + candidateId;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("文件大小不能超过 10MB");
        }
        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("仅支持 pdf、doc、docx 格式");
        }
    }

    private String resolveResumeName(MultipartFile file, String resumeName) {
        if (StringUtils.hasText(resumeName)) {
            return resumeName.trim();
        }
        String original = file.getOriginalFilename();
        if (StringUtils.hasText(original)) {
            return original;
        }
        return "我的简历";
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
