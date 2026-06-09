package com.talent.interview.controller;

import com.talent.common.api.R;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview")
public class InterviewHealthController {

    @GetMapping("/health")
    public R<Map<String, String>> health() {
        return R.ok(Map.of("service", "talent-interview", "status", "UP"));
    }
}
