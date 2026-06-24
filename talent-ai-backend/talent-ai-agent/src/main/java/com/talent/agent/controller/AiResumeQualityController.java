package com.talent.agent.controller;

import com.talent.agent.domain.dto.ResumeQualityEvaluateRequest;
import com.talent.agent.domain.vo.ResumeQualityScoreVO;
import com.talent.agent.service.AiResumeQualityService;
import com.talent.common.api.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/resume-score")
@RequiredArgsConstructor
public class AiResumeQualityController {

    private final AiResumeQualityService resumeQualityService;

    /** 查询简历最新质量评分（候选人 / HR 均可查看） */
    @GetMapping("/latest")
    public R<ResumeQualityScoreVO> latest(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("resumeId") Long resumeId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(resumeQualityService.getLatestByResumeId(resumeId));
    }

    /** 触发简历质量评分（仅候选人本人） */
    @PostMapping("/evaluate")
    public R<ResumeQualityScoreVO> evaluate(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody ResumeQualityEvaluateRequest request) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(resumeQualityService.evaluate(userId, request));
    }
}
