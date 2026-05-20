package com.talent.resume.controller;

import com.talent.common.api.R;
import com.talent.resume.service.ResumeService;
import com.talent.resume.vo.ResumePreviewVO;
import com.talent.resume.vo.ResumeUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历附件：MinIO 上传落库 + 私有桶预签名预览。
 */
@RestController
@RequestMapping("/api/resume/file")
@RequiredArgsConstructor
public class FileController {

    private final ResumeService resumeService;

    /**
     * 上传附件：写入 MinIO，并同步插入 resume / resume_attachment。
     */
    @PostMapping("/upload")
    public R<ResumeUploadVO> upload(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resumeId", required = false) Long resumeId,
            @RequestParam(value = "resumeName", required = false) String resumeName) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        try {
            return R.ok(resumeService.uploadResumeFile(userId, file, resumeId, resumeName));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 私有桶预签名预览（inline），候选人仅能访问本人附件，HR/管理员可访问全部。
     */
    @GetMapping("/preview/{attachmentId}")
    public R<ResumePreviewVO> preview(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long attachmentId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        try {
            return R.ok(resumeService.getPreviewByAttachmentId(userId, role, attachmentId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("获取预览链接失败：" + e.getMessage());
        }
    }

    /**
     * 按简历 ID 获取最新附件的预签名预览链接（候选人「我的投递」场景）。
     */
    @GetMapping("/preview/by-resume/{resumeId}")
    public R<ResumePreviewVO> previewByResume(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long resumeId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        try {
            return R.ok(resumeService.getPreviewByResumeId(userId, role, resumeId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("获取预览链接失败：" + e.getMessage());
        }
    }

    /** 兼容旧接口：返回预签名 URL 字符串 */
    @GetMapping("/download/{attachmentId}")
    public R<String> download(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long attachmentId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        try {
            return R.ok(resumeService.getDownloadUrl(userId, role, attachmentId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("获取下载链接失败：" + e.getMessage());
        }
    }
}
