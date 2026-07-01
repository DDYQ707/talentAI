package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.domain.entity.AiMatchRecord;
import com.talent.agent.domain.entity.AiParseTask;
import com.talent.agent.domain.vo.HrAiInsightsVO;
import com.talent.agent.mapper.AiMatchRecordMapper;
import com.talent.agent.mapper.AiParseTaskMapper;
import com.talent.agent.service.AiHrInsightsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiHrInsightsServiceImpl implements AiHrInsightsService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PROCESSING = 1;
    private static final int STATUS_SUCCESS = 2;
    private static final int STATUS_FAILED = 3;
    private static final int HIGH_MATCH_SCORE = 80;

    private final AiParseTaskMapper parseTaskMapper;
    private final AiMatchRecordMapper matchRecordMapper;

    @Override
    public HrAiInsightsVO getHrInsights() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        long parsedToday = countParseSuccessBetween(todayStart, tomorrowStart);
        long parsedThisMonth = countParseSuccessSince(monthStart);
        long highMatchCount = countHighMatchSince(monthStart);
        long matchSuccessThisMonth = countMatchSuccessSince(monthStart);
        long pendingAiTasks = countPendingParse() + countPendingMatch();
        long failedToday = countParseFailedBetween(todayStart, tomorrowStart)
                + countMatchFailedBetween(todayStart, tomorrowStart);

        long parseAttemptsThisMonth = countParseAttemptsSince(monthStart);
        long matchAttemptsThisMonth = countMatchAttemptsSince(monthStart);

        double parseRate = parseAttemptsThisMonth > 0
                ? parsedThisMonth / (double) parseAttemptsThisMonth
                : 0.0;
        double matchRate = matchAttemptsThisMonth > 0
                ? matchSuccessThisMonth / (double) matchAttemptsThisMonth
                : 0.0;
        double highMatchRate = matchSuccessThisMonth > 0
                ? highMatchCount / (double) matchSuccessThisMonth
                : 0.0;

        int healthScore = computeHealthScore(parseAttemptsThisMonth, matchAttemptsThisMonth, parseRate, matchRate, highMatchRate);
        String healthLabel = healthLabel(healthScore);
        String summary = buildSummary(parsedToday, highMatchCount, pendingAiTasks, failedToday, matchSuccessThisMonth);

        return HrAiInsightsVO.builder()
                .parsedToday(parsedToday)
                .parsedThisMonth(parsedThisMonth)
                .highMatchCount(highMatchCount)
                .matchSuccessThisMonth(matchSuccessThisMonth)
                .pendingAiTasks(pendingAiTasks)
                .failedToday(failedToday)
                .healthScore(healthScore)
                .healthLabel(healthLabel)
                .summary(summary)
                .build();
    }

    private long countParseSuccessBetween(LocalDateTime from, LocalDateTime to) {
        return parseTaskMapper.selectCount(new LambdaQueryWrapper<AiParseTask>()
                .eq(AiParseTask::getTaskStatus, STATUS_SUCCESS)
                .ge(AiParseTask::getCreatedAt, from)
                .lt(AiParseTask::getCreatedAt, to));
    }

    private long countParseSuccessSince(LocalDateTime from) {
        return parseTaskMapper.selectCount(new LambdaQueryWrapper<AiParseTask>()
                .eq(AiParseTask::getTaskStatus, STATUS_SUCCESS)
                .ge(AiParseTask::getCreatedAt, from));
    }

    private long countParseAttemptsSince(LocalDateTime from) {
        return parseTaskMapper.selectCount(new LambdaQueryWrapper<AiParseTask>()
                .ge(AiParseTask::getCreatedAt, from)
                .in(AiParseTask::getTaskStatus, List.of(STATUS_SUCCESS, STATUS_FAILED)));
    }

    private long countParseFailedBetween(LocalDateTime from, LocalDateTime to) {
        return parseTaskMapper.selectCount(new LambdaQueryWrapper<AiParseTask>()
                .eq(AiParseTask::getTaskStatus, STATUS_FAILED)
                .ge(AiParseTask::getCreatedAt, from)
                .lt(AiParseTask::getCreatedAt, to));
    }

    private long countPendingParse() {
        return parseTaskMapper.selectCount(new LambdaQueryWrapper<AiParseTask>()
                .in(AiParseTask::getTaskStatus, List.of(STATUS_PENDING, STATUS_PROCESSING)));
    }

    private long countMatchSuccessSince(LocalDateTime from) {
        return matchRecordMapper.selectCount(new LambdaQueryWrapper<AiMatchRecord>()
                .eq(AiMatchRecord::getMatchStatus, STATUS_SUCCESS)
                .ge(AiMatchRecord::getCreatedAt, from));
    }

    private long countHighMatchSince(LocalDateTime from) {
        return matchRecordMapper.selectCount(new LambdaQueryWrapper<AiMatchRecord>()
                .eq(AiMatchRecord::getMatchStatus, STATUS_SUCCESS)
                .ge(AiMatchRecord::getMatchScore, HIGH_MATCH_SCORE)
                .ge(AiMatchRecord::getCreatedAt, from));
    }

    private long countMatchAttemptsSince(LocalDateTime from) {
        return matchRecordMapper.selectCount(new LambdaQueryWrapper<AiMatchRecord>()
                .ge(AiMatchRecord::getCreatedAt, from)
                .in(AiMatchRecord::getMatchStatus, List.of(STATUS_SUCCESS, STATUS_FAILED)));
    }

    private long countMatchFailedBetween(LocalDateTime from, LocalDateTime to) {
        return matchRecordMapper.selectCount(new LambdaQueryWrapper<AiMatchRecord>()
                .eq(AiMatchRecord::getMatchStatus, STATUS_FAILED)
                .ge(AiMatchRecord::getCreatedAt, from)
                .lt(AiMatchRecord::getCreatedAt, to));
    }

    private long countPendingMatch() {
        return matchRecordMapper.selectCount(new LambdaQueryWrapper<AiMatchRecord>()
                .in(AiMatchRecord::getMatchStatus, List.of(STATUS_PENDING, STATUS_PROCESSING)));
    }

    private int computeHealthScore(
            long parseAttempts,
            long matchAttempts,
            double parseRate,
            double matchRate,
            double highMatchRate) {
        if (parseAttempts == 0 && matchAttempts == 0) {
            return 60;
        }
        int score = (int) Math.round(40 + parseRate * 20 + matchRate * 20 + highMatchRate * 20);
        return Math.min(100, Math.max(0, score));
    }

    private String healthLabel(int score) {
        if (score >= 85) {
            return "优秀";
        }
        if (score >= 70) {
            return "良好";
        }
        if (score >= 50) {
            return "一般";
        }
        return "待改善";
    }

    private String buildSummary(
            long parsedToday,
            long highMatchCount,
            long pendingAiTasks,
            long failedToday,
            long matchSuccessThisMonth) {
        List<String> parts = new ArrayList<>();
        if (parsedToday > 0) {
            parts.add("今日解析 " + parsedToday + " 份");
        }
        if (highMatchCount > 0) {
            parts.add("高匹配 " + highMatchCount + " 人");
        }
        if (matchSuccessThisMonth > 0) {
            parts.add("本月匹配 " + matchSuccessThisMonth + " 次");
        }
        if (pendingAiTasks > 0) {
            parts.add("待处理 " + pendingAiTasks + " 项");
        }
        if (failedToday > 0) {
            parts.add("今日失败 " + failedToday + " 项");
        }
        if (parts.isEmpty()) {
            return "暂无 AI 任务记录，投递简历后将自动触发解析与匹配";
        }
        return String.join(" · ", parts);
    }
}
