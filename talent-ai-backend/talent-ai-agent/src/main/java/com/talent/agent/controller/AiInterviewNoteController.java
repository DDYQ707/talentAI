package com.talent.agent.controller;

import com.talent.agent.domain.dto.InterviewNoteSaveRequest;
import com.talent.agent.domain.dto.InterviewNoteSynthesizeRequest;
import com.talent.agent.domain.vo.InterviewNoteVO;
import com.talent.agent.service.AiInterviewNoteService;
import com.talent.common.api.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/interview-notes")
@RequiredArgsConstructor
public class AiInterviewNoteController {

    private final AiInterviewNoteService interviewNoteService;

    /** 查询某次面试的笔记与 AI 评估草稿 */
    @GetMapping
    public R<InterviewNoteVO> getByInterview(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam("interviewId") Long interviewId) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(interviewNoteService.getByInterview(interviewId, userId));
    }

    /** 保存面试笔记（不触发 AI） */
    @PutMapping
    public R<InterviewNoteVO> save(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody InterviewNoteSaveRequest request) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(interviewNoteService.save(request, userId));
    }

    /** 根据笔记生成 AI 评估草稿并持久化 */
    @PostMapping("/synthesize")
    public R<InterviewNoteVO> synthesize(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody InterviewNoteSynthesizeRequest request) {
        if (userId == null) {
            return R.fail("未登录或用户信息缺失");
        }
        return R.ok(interviewNoteService.synthesize(request, userId));
    }
}
