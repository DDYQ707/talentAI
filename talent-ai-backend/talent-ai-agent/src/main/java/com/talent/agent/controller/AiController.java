package com.talent.agent.controller;

import com.talent.agent.domain.dto.AiMatchTriggerRequest;
import com.talent.agent.domain.dto.AiParseRetryRequest;
import com.talent.agent.domain.vo.HrAiInsightsVO;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.domain.vo.ParseTaskVO;
import com.talent.agent.service.AiHrInsightsService;
import com.talent.agent.service.AiMatchPreviewService;
import com.talent.agent.service.AiMatchService;
import com.talent.agent.service.AiParseService;
import com.talent.common.api.R;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final AiMatchPreviewService aiMatchPreviewService;
    private final AiHrInsightsService aiHrInsightsService;

    @GetMapping("/health")
    public R<Map<String, String>> health() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("service", "talent-ai-agent");
        info.put("status", "UP");
        return R.ok(info);
    }

    /** HR 工作台 — AI 今日洞察 */
    @GetMapping("/hr/insights")
    public R<HrAiInsightsVO> hrInsights(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(aiHrInsightsService.getHrInsights());
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

    /** HR 手动重新解析简历（覆盖最新解析结果并回填详情） */
    @PostMapping("/parse/retry")
    public R<ParseTaskVO> retryParse(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody AiParseRetryRequest request) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        if (request == null || request.getResumeId() == null) {
            return R.fail("resumeId 不能为空");
        }
        try {
            return R.ok(aiParseService.submitReparse(request));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
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

    /** HR 手动触发人岗匹配分析 */
    @PostMapping("/match/trigger")
    public R<MatchResultVO> triggerMatch(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody AiMatchTriggerRequest request) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        if (request == null || request.getResumeId() == null || request.getJobId() == null) {
            return R.fail("resumeId、jobId 不能为空");
        }
        try {
            return R.ok(aiMatchService.triggerMatchForHr(request));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    /** 候选人：查询岗位预览匹配（不触发新任务） */
    @GetMapping("/match/preview")
    public R<MatchResultVO> previewMatch(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("jobId") Long jobId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(aiMatchPreviewService.getPreviewMatch(userId, jobId));
    }

    /** 候选人：批量查询预览匹配缓存 */
    @GetMapping("/match/preview/batch")
    public R<Map<Long, MatchResultVO>> previewMatchBatch(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("jobIds") String jobIds) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        List<Long> ids = parseJobIds(jobIds);
        return R.ok(aiMatchPreviewService.getPreviewBatch(userId, ids));
    }

    /** 候选人：触发岗位预览匹配（无需投递） */
    @PostMapping("/match/preview")
    public R<MatchResultVO> triggerPreviewMatch(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("jobId") Long jobId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        try {
            return R.ok(aiMatchPreviewService.triggerPreviewMatch(userId, jobId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    private List<Long> parseJobIds(String jobIds) {
        if (jobIds == null || jobIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(jobIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
