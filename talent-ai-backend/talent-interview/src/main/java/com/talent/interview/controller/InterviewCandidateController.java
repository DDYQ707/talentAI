package com.talent.interview.controller;

import com.talent.common.api.R;
import com.talent.interview.service.InterviewService;
import com.talent.interview.vo.InterviewDetailVO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview/candidate")
@RequiredArgsConstructor
public class InterviewCandidateController {

    private final InterviewService interviewService;

    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long candidateId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status) {
        try {
            return R.ok(interviewService.pageForCandidate(role, candidateId, page, size, keyword, status));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/{interviewId}")
    public R<InterviewDetailVO> detail(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long candidateId,
            @PathVariable("interviewId") Long interviewId) {
        try {
            return R.ok(interviewService.detailForCandidate(role, candidateId, interviewId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
