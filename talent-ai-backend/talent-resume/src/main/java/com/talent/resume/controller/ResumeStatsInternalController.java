package com.talent.resume.controller;

import com.talent.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 供 talent-analytics 聚合统计（Feign 内部调用） */
@RestController
@RequestMapping("/api/resume/internal/stats")
@RequiredArgsConstructor
public class ResumeStatsInternalController {

    private final ResumeMapper resumeMapper;

    @GetMapping("/total-count")
    public Long totalCount() {
        return resumeMapper.selectCount(null);
    }
}
