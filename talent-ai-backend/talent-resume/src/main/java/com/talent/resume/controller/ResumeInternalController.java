package com.talent.resume.controller;

import com.talent.resume.dto.OnDeliveryScreenRequest;
import com.talent.resume.service.ResumeService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

    /** 校验简历归属并返回主简历 ID */
    @GetMapping("/ownership")
    public Map<String, Object> ownership(@RequestParam("resumeId") Long resumeId) {
        return resumeService.getResumeOwnership(resumeId);
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
}
