package com.talent.resume.controller;

import com.talent.common.api.R;
import com.talent.resume.service.ResumeService;
import com.talent.resume.vo.ResumeListVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    /** 附件简历列表（投递用） */
    @GetMapping("/attachment/my")
    public R<List<ResumeListVO>> attachmentResumes(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(resumeService.listAttachmentResumes(userId));
    }

    /** 兼容旧路径，等同 attachment/my */
    @GetMapping("/my")
    public R<List<ResumeListVO>> myResumes(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(resumeService.listAttachmentResumes(userId));
    }

    @GetMapping("/brief/{resumeId}")
    public R<ResumeListVO> brief(@PathVariable("resumeId") Long resumeId) {
        ResumeListVO vo = resumeService.getAttachmentBriefByResumeId(resumeId);
        if (vo == null) {
            return R.fail("简历不存在");
        }
        return R.ok(vo);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("id") Long id) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        try {
            resumeService.deleteResume(userId, id);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
