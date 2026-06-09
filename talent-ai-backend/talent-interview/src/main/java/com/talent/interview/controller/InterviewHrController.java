package com.talent.interview.controller;

import com.talent.common.api.R;
import com.talent.interview.dto.InterviewScheduleRequest;
import com.talent.interview.service.InterviewService;
import com.talent.interview.vo.InterviewDetailVO;
import com.talent.interview.vo.InterviewListVO;
import com.talent.interview.vo.InterviewScheduleResultVO;
import com.talent.interview.vo.InterviewStatsVO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview/hr")
@RequiredArgsConstructor
public class InterviewHrController {

    private final InterviewService interviewService;

    @PostMapping("/schedule")
    public R<InterviewScheduleResultVO> schedule(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long hrId,
            @RequestHeader(value = "X-User-Name", required = false) String hrName,
            @RequestBody InterviewScheduleRequest request) {
        try {
            return R.ok(interviewService.schedule(role, hrId, hrName, request));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate dateTo) {
        try {
            return R.ok(interviewService.pageForHr(role, page, size, keyword, status, dateFrom, dateTo));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public R<InterviewStatsVO> stats(@RequestHeader(value = "X-User-Role", required = false) String role) {
        try {
            return R.ok(interviewService.statsForHr(role));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/{interviewId}")
    public R<InterviewDetailVO> detail(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("interviewId") Long interviewId) {
        try {
            return R.ok(interviewService.detailForHr(role, interviewId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @PutMapping("/{interviewId}/cancel")
    public R<Void> cancel(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("interviewId") Long interviewId) {
        try {
            interviewService.cancelForHr(role, interviewId);
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/by-application")
    public R<List<InterviewListVO>> listByApplication(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam("applicationId") Long applicationId) {
        try {
            return R.ok(interviewService.listByApplicationForHr(role, applicationId));
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }
}
