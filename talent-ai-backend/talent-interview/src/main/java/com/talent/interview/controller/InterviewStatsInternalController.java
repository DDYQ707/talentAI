package com.talent.interview.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.interview.constant.InterviewConstants;
import com.talent.interview.entity.Interview;
import com.talent.interview.mapper.InterviewMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 供 talent-analytics 聚合统计（Feign 内部调用） */
@RestController
@RequestMapping("/api/interview/internal/stats")
@RequiredArgsConstructor
public class InterviewStatsInternalController {

    private final InterviewMapper interviewMapper;

    @GetMapping("/ongoing-count")
    public Long countOngoingInterviews() {
        return interviewMapper.selectCount(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getStatus, InterviewConstants.STATUS_PENDING));
    }

    @GetMapping("/completed-this-month")
    public Long countCompletedThisMonth() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return interviewMapper.selectCount(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getStatus, InterviewConstants.STATUS_COMPLETED)
                .ge(Interview::getUpdatedAt, monthStart));
    }

    @GetMapping("/passed-by-month")
    public Long countPassedInterviews(@RequestParam("yearMonth") String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        return interviewMapper.selectCount(new LambdaQueryWrapper<Interview>()
                .eq(Interview::getStatus, InterviewConstants.STATUS_COMPLETED)
                .ge(Interview::getUpdatedAt, start)
                .lt(Interview::getUpdatedAt, end));
    }
}
