package com.talent.resume.controller;

import com.talent.resume.dto.InternalScreenSyncRequest;
import com.talent.resume.dto.OnDeliveryScreenRequest;
import com.talent.resume.service.ResumeService;
import com.talent.resume.vo.HrResumeDetailVO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 微服务内部调用（Feign） */
@RestController
@RequestMapping("/api/resume/internal")
@RequiredArgsConstructor
public class ResumeInternalController {

    private final ResumeService resumeService;

    /** AI 助手：分页搜索 HR 简历列表 */
    @GetMapping("/hr/page")
    public Map<String, Object> hrPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "screenStatus", required = false) Integer screenStatus,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            Map<String, Object> data = resumeService.pageHrResumes("HR", current, size, keyword, screenStatus);
            return Map.of("code", 200, "msg", "ok", "data", data);
        } catch (Exception e) {
            return Map.of("code", 500, "msg", "简历搜索失败：" + e.getMessage());
        }
    }

    /** AI 助手：简历详情摘要 */
    @GetMapping("/hr/{resumeId}/brief")
    public Map<String, Object> hrResumeBrief(@PathVariable("resumeId") Long resumeId) {
        if (resumeId == null) {
            return Map.of("code", 400, "msg", "resumeId 不能为空");
        }
        try {
            HrResumeDetailVO detail = resumeService.getHrResumeDetail("HR", resumeId);
            return Map.of("code", 200, "msg", "ok", "data", detail);
        } catch (IllegalArgumentException e) {
            return Map.of("code", 404, "msg", e.getMessage());
        }
    }

    /** 校验简历归属并返回主简历 ID */
    @GetMapping("/ownership")
    public Map<String, Object> ownership(@RequestParam("resumeId") Long resumeId) {
        return resumeService.getResumeOwnership(resumeId);
    }

    /** 按附件 ID 查询存储信息（供 AI 解析服务内部调用） */
    @GetMapping("/attachment/{attachmentId}")
    public Map<String, Object> attachment(@PathVariable("attachmentId") Long attachmentId) {
        try {
            Map<String, Object> data = resumeService.getAttachmentForInternal(attachmentId);
            return Map.of("code", 200, "msg", "ok", "data", data);
        } catch (IllegalArgumentException e) {
            return Map.of("code", 400, "msg", e.getMessage());
        }
    }

    /** 供 AI 解析：附件摘要或在线简历结构化数据 */
    @GetMapping("/ai-parse-context")
    public Map<String, Object> aiParseContext(@RequestParam("resumeId") Long resumeId) {
        if (resumeId == null) {
            return Map.of("code", 400, "msg", "resumeId 不能为空");
        }
        try {
            return Map.of("code", 200, "msg", "ok", "data", resumeService.getAiParseContext(resumeId));
        } catch (IllegalArgumentException e) {
            return Map.of("code", 400, "msg", e.getMessage());
        }
    }

    /** 候选人投递成功后，将主简历设为待初筛 */
    @PostMapping("/on-delivery")
    public Map<String, Object> onDelivery(@RequestBody OnDeliveryScreenRequest request) {
        if (request == null || request.getResumeId() == null || request.getCandidateId() == null) {
            return Map.of("code", 400, "msg", "resumeId 与 candidateId 不能为空");
        }
        try {
            Long primaryResumeId = resumeService.markPendingOnDelivery(request.getResumeId(), request.getCandidateId());
            return Map.of("code", 200, "msg", "ok", "primaryResumeId", primaryResumeId);
        } catch (IllegalArgumentException e) {
            return Map.of("code", 400, "msg", e.getMessage());
        }
    }

    /** 微服务内部：同步主简历筛选状态 */
    @PostMapping("/sync-screen-status")
    public Map<String, Object> syncScreenStatus(@RequestBody InternalScreenSyncRequest request) {
        if (request == null || request.getCandidateId() == null || request.getScreenStatus() == null) {
            return Map.of("code", 400, "msg", "candidateId 与 screenStatus 不能为空");
        }
        try {
            resumeService.internalSyncScreenStatus(
                    request.getResumeId(),
                    request.getCandidateId(),
                    request.getScreenStatus(),
                    request.getOperatorId(),
                    request.getRemark());
            return Map.of("code", 200, "msg", "ok");
        } catch (IllegalArgumentException e) {
            return Map.of("code", 400, "msg", e.getMessage());
        }
    }
}
