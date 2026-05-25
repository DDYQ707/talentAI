package com.talent.auth.controller;

import com.talent.auth.service.CandidateMyProfileService;
import com.talent.common.api.R;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HR 查看候选人档案（走网关，不依赖 resume 服务 Feign）。
 */
@RestController
@RequestMapping("/api/auth/hr")
@RequiredArgsConstructor
public class HrCandidateController {

    private final CandidateMyProfileService candidateMyProfileService;

    @GetMapping("/candidate-brief")
    public R<Map<String, Object>> candidateBrief(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam("userId") Long userId) {
        if (!isHrOrAdmin(role)) {
            return R.fail("仅 HR 或管理员可访问");
        }
        return R.ok(candidateMyProfileService.getProfileBrief(userId));
    }

    private boolean isHrOrAdmin(String role) {
        if (role == null) {
            return false;
        }
        return "HR".equalsIgnoreCase(role.trim()) || "ADMIN".equalsIgnoreCase(role.trim());
    }
}
