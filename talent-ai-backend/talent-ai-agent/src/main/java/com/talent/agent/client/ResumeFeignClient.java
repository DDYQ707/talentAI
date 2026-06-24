package com.talent.agent.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "talent-resume")
public interface ResumeFeignClient {

    @GetMapping("/api/resume/internal/hr/page")
    Map<String, Object> searchHrResumes(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "screenStatus", required = false) Integer screenStatus,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size);

    @GetMapping("/api/resume/internal/hr/{resumeId}/brief")
    Map<String, Object> getHrResumeBrief(@PathVariable("resumeId") Long resumeId);

    @GetMapping("/api/resume/brief/{resumeId}")
    Map<String, Object> getResumeBrief(@PathVariable("resumeId") Long resumeId);

    @GetMapping("/api/resume/internal/ownership")
    Map<String, Object> getResumeOwnership(@RequestParam("resumeId") Long resumeId);

    @GetMapping("/api/resume/internal/attachment/{attachmentId}")
    Map<String, Object> getAttachmentById(@PathVariable("attachmentId") Long attachmentId);

    @GetMapping("/api/resume/internal/primary-by-candidate")
    Map<String, Object> getPrimaryByCandidate(@RequestParam("candidateId") Long candidateId);

    @GetMapping("/api/resume/internal/ai-parse-context")
    Map<String, Object> getAiParseContext(@RequestParam("resumeId") Long resumeId);
}
