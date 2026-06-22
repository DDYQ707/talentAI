package com.talent.agent.controller;

import com.talent.agent.domain.dto.ProfileGenerateRequest;
import com.talent.agent.domain.vo.TalentProfileVO;
import com.talent.agent.service.AiTalentProfileService;
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
@RequestMapping("/api/ai/profile")
@RequiredArgsConstructor
public class AiProfileController {

    private final AiTalentProfileService talentProfileService;

    /** 生成/刷新 AI 人才画像（HR 手动或 job 状态变更 Feign 触发） */
    @PostMapping("/generate")
    public R<TalentProfileVO> generate(@RequestBody ProfileGenerateRequest request) {
        return R.ok(talentProfileService.generate(request));
    }

    /** HR 查询某投递的最新人才画像 */
    @GetMapping("/by-application")
    public R<TalentProfileVO> getByApplication(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("applicationId") Long applicationId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(talentProfileService.getByApplicationId(applicationId));
    }
}
