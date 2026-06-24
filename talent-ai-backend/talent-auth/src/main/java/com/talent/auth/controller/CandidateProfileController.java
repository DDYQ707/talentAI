package com.talent.auth.controller;

import com.talent.auth.dto.CandidateProfileSaveRequest;
import com.talent.auth.service.CandidateMyProfileService;
import com.talent.auth.vo.CandidateProfileCompletenessVO;
import com.talent.auth.vo.CandidateProfileVO;
import com.talent.common.api.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/candidate/profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateMyProfileService candidateMyProfileService;

    @GetMapping("/my")
    public R<CandidateProfileVO> myProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return candidateMyProfileService.getMyProfile(userId);
    }

    @PutMapping("/my")
    public R<CandidateProfileVO> saveMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody CandidateProfileSaveRequest request) {
        return candidateMyProfileService.saveMyProfile(userId, request);
    }

    @GetMapping("/complete")
    public R<CandidateProfileCompletenessVO> completeness(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return candidateMyProfileService.getCompleteness(userId);
    }

    /** 微服务内部：档案摘要 */
    @GetMapping("/brief")
    public Map<String, Object> profileBrief(@RequestParam("userId") Long userId) {
        return candidateMyProfileService.getProfileBrief(userId);
    }

    /** 微服务内部：同步 AI 简历质量评分到候选人档案 */
    @PutMapping("/internal/ai-score")
    public R<Void> syncAiScore(@RequestParam("userId") Long userId, @RequestParam("aiScore") Integer aiScore) {
        return candidateMyProfileService.syncAiScore(userId, aiScore);
    }
}
