package com.talent.resume.controller;

import com.talent.common.api.R;
import com.talent.resume.service.ResumeService;
import com.talent.resume.vo.HrResumeDetailVO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HR 简历管理：列表、详情（预览走 FileController 预签名接口）。
 */
@RestController
@RequestMapping("/api/resume/hr")
@RequiredArgsConstructor
public class HrResumeController {

    private final ResumeService resumeService;

    /** 手动触发：合并库中同一候选人的重复简历 */
    @PostMapping("/consolidate")
    public R<Map<String, Object>> consolidate(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        try {
            int removed = resumeService.hrConsolidateDuplicates(role);
            return R.ok(Map.of("mergedDuplicates", removed));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(value = "current", required = false) Integer current,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "screenStatus", required = false) Integer screenStatus) {
        try {
            return R.ok(resumeService.pageHrResumes(role, current, size, keyword, screenStatus));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/{resumeId}")
    public R<HrResumeDetailVO> detail(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable Long resumeId) {
        try {
            return R.ok(resumeService.getHrResumeDetail(role, resumeId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
