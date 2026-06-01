package com.talent.agent.controller;

import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.domain.vo.ParseTaskVO;
import com.talent.agent.service.AiMatchService;
import com.talent.agent.service.AiParseService;
import com.talent.common.api.R;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiParseService aiParseService;
    private final AiMatchService aiMatchService;

    @GetMapping("/health")
    public R<Map<String, String>> health() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("service", "talent-ai-agent");
        info.put("status", "UP");
        return R.ok(info);
    }

    /** HR 查询简历最新解析任务状态 */
    @GetMapping("/parse/latest")
    public R<ParseTaskVO> latestParse(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("resumeId") Long resumeId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(aiParseService.getLatestByResumeId(resumeId));
    }

    /** HR 查询投递匹配结果 */
    @GetMapping("/match/by-application")
    public R<MatchResultVO> matchByApplication(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("applicationId") Long applicationId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(aiMatchService.getByApplicationId(applicationId));
    }

    /** HR 按简历+岗位查询最新匹配结果 */
    @GetMapping("/match/latest")
    public R<MatchResultVO> latestMatch(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("resumeId") Long resumeId,
            @RequestParam("jobId") Long jobId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(aiMatchService.getLatestByResumeAndJob(resumeId, jobId));
    }
}
