package com.talent.agent.controller;

import com.talent.agent.domain.dto.InterviewQuestionGenerateRequest;
import com.talent.agent.domain.vo.InterviewQuestionGenerateResultVO;
import com.talent.agent.domain.vo.InterviewQuestionVO;
import com.talent.agent.service.AiInterviewQuestionService;
import com.talent.common.api.R;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/interview-questions")
@RequiredArgsConstructor
public class AiInterviewQuestionController {

    private final AiInterviewQuestionService interviewQuestionService;

    /** 同步生成针对性面试题并持久化到 ai_interview_question */
    @PostMapping("/generate")
    public R<InterviewQuestionGenerateResultVO> generate(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody InterviewQuestionGenerateRequest request) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(interviewQuestionService.generate(request));
    }

    /** 查询某次面试已生成的 AI 面试题 */
    @GetMapping
    public R<List<InterviewQuestionVO>> listByInterview(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("interviewId") Long interviewId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(interviewQuestionService.listByInterviewId(interviewId));
    }
}
