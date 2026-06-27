package com.talent.job.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.job.constant.JobApplicationConstants;
import com.talent.job.constant.OfferConstants;
import com.talent.job.entity.JobApplication;
import com.talent.job.entity.JobPost;
import com.talent.job.entity.Offer;
import com.talent.job.mapper.OfferMapper;
import com.talent.job.service.IJobApplicationService;
import com.talent.job.service.IJobPostService;
import com.talent.job.vo.DepartmentJobStatVO;
import com.talent.job.vo.MonthlyCountVO;
import com.talent.job.vo.OfferStatsVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 供 talent-analytics 聚合统计 */
@RestController
@RequestMapping("/api/job/internal/stats")
@RequiredArgsConstructor
public class JobStatsInternalController {

    private final IJobPostService jobPostService;
    private final IJobApplicationService jobApplicationService;
    private final OfferMapper offerMapper;

    @GetMapping("/active-job-count")
    public Long countActiveJobs() {
        return jobPostService.count(new LambdaQueryWrapper<JobPost>()
                .eq(JobPost::getStatus, JobApplicationConstants.JOB_STATUS_OPEN));
    }

    @GetMapping("/monthly-application-count")
    public Long countMonthlyApplications() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return jobApplicationService.count(new LambdaQueryWrapper<JobApplication>()
                .ge(JobApplication::getAppliedAt, monthStart));
    }

    @GetMapping("/application-count-by-stage")
    public Map<Integer, Long> countApplicationsByStage(@RequestParam("stages") List<Integer> stages) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        if (stages == null || stages.isEmpty()) {
            return result;
        }
        for (Integer stage : stages) {
            if (stage == null) {
                continue;
            }
            long count = jobApplicationService.count(new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getCurrentStage, stage.byteValue()));
            result.put(stage, count);
        }
        return result;
    }

    @GetMapping("/application-count-by-status")
    public Map<Integer, Long> countApplicationsByStatus(@RequestParam("statuses") List<Integer> statuses) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        if (statuses == null || statuses.isEmpty()) {
            return result;
        }
        for (Integer status : statuses) {
            if (status == null) {
                continue;
            }
            long count = jobApplicationService.count(new LambdaQueryWrapper<JobApplication>()
                    .eq(JobApplication::getStatus, status.byteValue()));
            result.put(status, count);
        }
        return result;
    }

    /** Offer 看板指标：本月发放量 + 本月候选人接受率 */
    @GetMapping("/offer-metrics")
    public OfferStatsVO offerMetrics() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        List<Byte> sentStatuses = List.of(
                OfferConstants.OFFER_STATUS_APPROVED,
                OfferConstants.OFFER_STATUS_ISSUED,
                OfferConstants.OFFER_STATUS_ACCEPTED,
                OfferConstants.OFFER_STATUS_DECLINED);

        Long monthlyOfferSent = offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .in(Offer::getStatus, sentStatuses)
                .ge(Offer::getCreatedAt, monthStart));

        Long acceptedThisMonth = offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStatus, OfferConstants.OFFER_STATUS_ACCEPTED)
                .ge(Offer::getUpdatedAt, monthStart));

        Long declinedThisMonth = offerMapper.selectCount(new LambdaQueryWrapper<Offer>()
                .eq(Offer::getStatus, OfferConstants.OFFER_STATUS_DECLINED)
                .ge(Offer::getUpdatedAt, monthStart));

        long decided = acceptedThisMonth + declinedThisMonth;
        double offerAcceptRate = decided > 0
                ? acceptedThisMonth.doubleValue() / decided
                : 0.0;

        return OfferStatsVO.builder()
                .monthlyOfferSent(monthlyOfferSent)
                .offerAcceptRate(offerAcceptRate)
                .build();
    }

    /**
     * 最近N个月的每月投递量
     */
    @GetMapping("/monthly-applications")
    public List<MonthlyCountVO> getMonthlyApplications(@RequestParam(defaultValue = "6") int months) {
        LocalDateTime startDate = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1).atStartOfDay();
        List<JobApplication> apps = jobApplicationService.list(
                new LambdaQueryWrapper<JobApplication>()
                        .ge(JobApplication::getAppliedAt, startDate));

        Map<String, Long> grouped = apps.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.counting()));

        return buildMonthlyList(months, grouped);
    }

    /**
     * 最近N个月的每月Offer发放量（已发放状态：已通过/已发放/已接受/已拒绝）
     */
    @GetMapping("/monthly-offers")
    public List<MonthlyCountVO> getMonthlyOffers(@RequestParam(defaultValue = "6") int months) {
        LocalDateTime startDate = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1).atStartOfDay();
        List<Byte> sentStatuses = List.of(
                OfferConstants.OFFER_STATUS_APPROVED,
                OfferConstants.OFFER_STATUS_ISSUED,
                OfferConstants.OFFER_STATUS_ACCEPTED,
                OfferConstants.OFFER_STATUS_DECLINED);

        List<Offer> offers = offerMapper.selectList(
                new LambdaQueryWrapper<Offer>()
                        .in(Offer::getStatus, sentStatuses)
                        .ge(Offer::getCreatedAt, startDate));

        Map<String, Long> grouped = offers.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.counting()));

        return buildMonthlyList(months, grouped);
    }

    /**
     * 按部门统计：缺口（headcount总和）和在招岗位数
     */
    @GetMapping("/department-job-stats")
    public List<DepartmentJobStatVO> getDepartmentJobStats() {
        List<JobPost> allJobs = jobPostService.list(new LambdaQueryWrapper<JobPost>());

        Map<Long, List<JobPost>> byDept = allJobs.stream()
                .collect(Collectors.groupingBy(JobPost::getDeptId));

        return byDept.entrySet().stream()
                .map(entry -> {
                    List<JobPost> deptJobs = entry.getValue();
                    String deptName = deptJobs.stream()
                            .map(JobPost::getDeptName)
                            .filter(name -> name != null && !name.isEmpty())
                            .findFirst().orElse("未知部门");
                    int headcountSum = deptJobs.stream()
                            .mapToInt(j -> j.getHeadcount() != null ? j.getHeadcount() : 0)
                            .sum();
                    long activeCount = deptJobs.stream()
                            .filter(j -> j.getStatus() != null && j.getStatus() == JobApplicationConstants.JOB_STATUS_OPEN)
                            .count();
                    return DepartmentJobStatVO.builder()
                            .deptId(entry.getKey())
                            .deptName(deptName)
                            .headcount(headcountSum)
                            .activeCount(activeCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建填充了缺失月份的月度列表
     */
    private List<MonthlyCountVO> buildMonthlyList(int months, Map<String, Long> grouped) {
        List<MonthlyCountVO> result = new ArrayList<>();
        YearMonth current = YearMonth.now().minusMonths(months - 1);
        YearMonth end = YearMonth.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        while (!current.isAfter(end)) {
            String key = current.format(fmt);
            result.add(new MonthlyCountVO(key, grouped.getOrDefault(key, 0L)));
            current = current.plusMonths(1);
        }
        return result;
    }
}
