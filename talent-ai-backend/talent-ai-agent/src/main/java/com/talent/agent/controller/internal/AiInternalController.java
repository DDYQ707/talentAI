package com.talent.agent.controller.internal;

import com.talent.agent.domain.dto.MatchRequest;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.vo.MatchResultVO;
import com.talent.agent.domain.vo.ParseTaskVO;
import com.talent.agent.service.AiMatchService;
import com.talent.agent.service.AiParseService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 微服务内部调用（Feign），路径 /internal/ai/** */
@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
public class AiInternalController {

    private final AiParseService aiParseService;
    private final AiMatchService aiMatchService;

    /** 投递后触发简历解析任务（异步闭环入口，Sprint 1 先创建待处理任务） */
    @PostMapping("/parse/submit")
    public Map<String, Object> submitParse(@RequestBody ParseTaskRequest request) {
        try {
            ParseTaskVO vo = aiParseService.submitParseTask(request);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("taskId", vo.getTaskId());
            data.put("resumeId", vo.getResumeId());
            data.put("taskStatus", vo.getTaskStatus());
            return Map.of("code", 200, "msg", "ok", "data", data);
        } catch (IllegalArgumentException e) {
            return Map.of("code", 400, "msg", e.getMessage());
        }
    }

    @GetMapping("/parse/task")
    public Map<String, Object> getParseTask(@RequestParam("taskId") Long taskId) {
        try {
            ParseTaskVO vo = aiParseService.getParseTask(taskId);
            return Map.of("code", 200, "msg", "ok", "data", vo);
        } catch (IllegalArgumentException e) {
            return Map.of("code", 400, "msg", e.getMessage());
        }
    }

    @GetMapping("/parse/latest-by-resume")
    public Map<String, Object> latestParseByResume(@RequestParam("resumeId") Long resumeId) {
        try {
            ParseTaskVO vo = aiParseService.getLatestByResumeId(resumeId);
            return Map.of("code", 200, "msg", "ok", "data", vo);
        } catch (IllegalArgumentException e) {
            return Map.of("code", 400, "msg", e.getMessage());
        }
    }

    /** 解析完成后触发人岗匹配（Sprint 1 先创建占位匹配记录） */
    @PostMapping("/match/submit")
    public Map<String, Object> submitMatch(@RequestBody MatchRequest request) {
        try {
            MatchResultVO vo = aiMatchService.submitMatch(request);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("matchId", vo.getMatchId());
            data.put("applicationId", vo.getApplicationId());
            data.put("matchScore", vo.getMatchScore());
            return Map.of("code", 200, "msg", "ok", "data", data);
        } catch (IllegalArgumentException e) {
            return Map.of("code", 400, "msg", e.getMessage());
        }
    }

    @GetMapping("/match/by-application")
    public Map<String, Object> matchByApplication(@RequestParam("applicationId") Long applicationId) {
        try {
            MatchResultVO vo = aiMatchService.getByApplicationId(applicationId);
            return Map.of("code", 200, "msg", "ok", "data", vo);
        } catch (IllegalArgumentException e) {
            return Map.of("code", 400, "msg", e.getMessage());
        }
    }
}
