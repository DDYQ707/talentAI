package com.talent.resume.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.talent.resume.constant.ResumeConstants;
import com.talent.resume.entity.Resume;
import com.talent.resume.entity.ResumeAttachment;
import com.talent.resume.feign.AuthFeignClient;
import com.talent.resume.mapper.ResumeAttachmentMapper;
import com.talent.resume.mapper.ResumeMapper;
import com.talent.resume.service.MinioStorageService.StoredObject;
import com.talent.resume.vo.HrResumeDetailVO;
import com.talent.resume.vo.HrResumeListVO;
import com.talent.resume.vo.ResumeListVO;
import com.talent.resume.vo.ResumePreviewVO;
import com.talent.resume.vo.ResumeUploadVO;
import java.util.ArrayList;
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
    private final MinioStorageService minioStorageService;
    private final AuthFeignClient authFeignClient;

    /** 仅返回已上传附件的简历（用于投递选择） */
    public List<ResumeListVO> listAttachmentResumes(Long candidateId) {
        List<Resume> resumes = resumeMapper.selectList(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getCandidateId, candidateId)
                        .orderByDesc(Resume::getUpdatedAt));
        if (resumes.isEmpty()) {
            return List.of();
        }
        Map<Long, ResumeAttachment> latestByResume = loadLatestAttachments(
                resumes.stream().map(Resume::getId).toList());
        List<ResumeListVO> result = new ArrayList<>();
        for (Resume resume : resumes) {
            ResumeAttachment att = latestByResume.get(resume.getId());
            if (att == null) {
                continue;
            }
            result.add(toListVo(resume, att));
        }
        return result;
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
            resume = new Resume();
            resume.setCandidateId(candidateId);
            resume.setResumeName(resolveResumeName(file, resumeName));
            resume.setIsDefault(0);
            resume.setParseStatus(0);
            resume.setScreenStatus(1);
            resumeMapper.insert(resume);
        }

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

        Page<Resume> page = resumeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Long> resumeIds = page.getRecords().stream().map(Resume::getId).toList();
        Map<Long, ResumeAttachment> latestMap = loadLatestAttachments(resumeIds);

        List<HrResumeListVO> records = new ArrayList<>();
        for (Resume resume : page.getRecords()) {
            HrResumeListVO vo = new HrResumeListVO();
            vo.setId(resume.getId());
            vo.setCandidateId(resume.getCandidateId());
            vo.setCandidateName(resolveCandidateName(resume.getCandidateId()));
            vo.setResumeName(resume.getResumeName());
            vo.setScreenStatus(resume.getScreenStatus());
            vo.setParseStatus(resume.getParseStatus());
            vo.setUpdatedAt(resume.getUpdatedAt());
            ResumeAttachment att = latestMap.get(resume.getId());
            if (att != null) {
                vo.setAttachmentId(att.getId());
                vo.setFileName(att.getFileName());
                vo.setFileType(att.getFileType());
                vo.setFileSize(att.getFileSize());
            }
            records.add(vo);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("pages", page.getPages());
        data.put("records", records);
        return data;
    }

    public HrResumeDetailVO getHrResumeDetail(String role, Long resumeId) {
        assertHrRole(role);
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new IllegalArgumentException("简历不存在");
        }
        HrResumeDetailVO vo = new HrResumeDetailVO();
        vo.setId(resume.getId());
        vo.setCandidateId(resume.getCandidateId());
        vo.setCandidateName(resolveCandidateName(resume.getCandidateId()));
        vo.setResumeName(resume.getResumeName());
        vo.setSummary(resume.getSummary());
        vo.setIsDefault(resume.getIsDefault());
        vo.setParseStatus(resume.getParseStatus());
        vo.setScreenStatus(resume.getScreenStatus());
        vo.setCreatedAt(resume.getCreatedAt());
        vo.setUpdatedAt(resume.getUpdatedAt());
        ResumeAttachment att = getLatestAttachment(resumeId);
        if (att != null) {
            vo.setAttachmentId(att.getId());
            vo.setFileName(att.getFileName());
            vo.setFileType(att.getFileType());
            vo.setFileSize(att.getFileSize());
        }
        return vo;
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
        return ResumeConstants.ROLE_HR.equals(role) || ResumeConstants.ROLE_ADMIN.equals(role);
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
