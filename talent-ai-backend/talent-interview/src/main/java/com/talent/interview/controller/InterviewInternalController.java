package com.talent.interview.controller;

import com.talent.interview.entity.Interview;
import com.talent.interview.service.InterviewService;
import com.talent.interview.vo.InterviewBriefVO;
import com.talent.interview.vo.InterviewEvaluationVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 微服务内部调用（Feign），供 ai-agent 等拉取面试上下文 */
@RestController
@RequestMapping("/api/interview/internal")
@RequiredArgsConstructor
public class InterviewInternalController {

    private final InterviewService interviewService;

    @GetMapping("/{interviewId}/brief")
    public Map<String, Object> brief(@PathVariable("interviewId") Long interviewId) {
        Interview interview = interviewService.getById(interviewId);
        if (interview == null) {
            return Map.of("code", 404, "msg", "面试记录不存在");
        }
        InterviewBriefVO data = interviewService.toBrief(interview);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    @GetMapping("/by-application")
    public Map<String, Object> listByApplication(@RequestParam("applicationId") Long applicationId) {
        if (applicationId == null) {
            return Map.of("code", 400, "msg", "applicationId 不能为空");
        }
        List<InterviewBriefVO> records = interviewService.listByApplicationId(applicationId).stream()
                .map(interviewService::toBrief)
                .collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", records);
        return result;
    }

    /** 供 ai-agent 拉取某投递下全部面试评价 */
    @GetMapping("/evaluations-by-application")
    public Map<String, Object> evaluationsByApplication(@RequestParam("applicationId") Long applicationId) {
        if (applicationId == null) {
            return Map.of("code", 400, "msg", "applicationId 不能为空");
        }
        List<InterviewEvaluationVO> records = interviewService.listEvaluationsByApplication(applicationId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", records);
        return result;
    }
}
